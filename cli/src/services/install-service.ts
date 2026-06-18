import { mkdir, mkdtemp, rename, rm, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { SkillHubClient } from '../clients/skillhub-client'
import { InventoryStore } from '../stores/inventory-store'
import { CliError } from '../shared/errors'
import { EXIT } from '../shared/constants'
import { extractZip } from '../platform/archive'
import { readBoundedResponseBody } from '../platform/download'
import { pathExists } from '../platform/paths'
import type { AgentCandidate } from '../agents/types'

export interface InstallOptions {
  registry: string
  token?: string | undefined
  namespace: string
  slug: string
  version?: string | undefined
  targets: AgentCandidate[]
  force: boolean
  home?: string | undefined
}

export async function installSkill(options: InstallOptions): Promise<{ installed: Array<{ agent: string; dir: string }> }> {
  const client = new SkillHubClient(options.registry, options.token)
  const resolved = await client.resolve(options.namespace, options.slug, options.version)
  const response = await client.download(options.namespace, options.slug, resolved.version)
  const buffer = await readBoundedResponseBody(response)

  const installed: Array<{ agent: string; dir: string }> = []
  const store = new InventoryStore(options.home)

  for (const target of options.targets) {
    const skillDir = join(target.rootDir, options.slug)

    if (await pathExists(skillDir) && !options.force) {
      throw new CliError(`skill already installed at ${skillDir}`, EXIT.filesystem, {
        path: skillDir,
        next: 'pass --force to overwrite'
      })
    }

    await mkdir(target.rootDir, { recursive: true })
    const tempDir = await mkdtemp(join(target.rootDir, `.${options.slug}.install-`))
    let movedIntoPlace = false

    try {
      await extractZip(buffer, tempDir)

      const installedAt = new Date().toISOString()
      const metaDir = join(tempDir, '.skillhub')
      await mkdir(metaDir, { recursive: true })
      await writeFile(join(metaDir, 'metadata.json'), JSON.stringify({
        registry: options.registry,
        namespace: options.namespace,
        slug: options.slug,
        version: resolved.version,
        agent: target.agent,
        installedAt
      }, null, 2))

      if (await pathExists(skillDir) && !options.force) {
        throw new CliError(`skill already installed at ${skillDir}`, EXIT.filesystem, {
          path: skillDir,
          next: 'pass --force to overwrite'
        })
      }

      if (await pathExists(skillDir) && options.force) {
        await store.removeTargetsByInstallDir(skillDir)
        await rm(skillDir, { recursive: true, force: true })
      }

      try {
        await rename(tempDir, skillDir)
      } catch (error) {
        if (!options.force && await pathExists(skillDir)) {
          throw new CliError(`skill already installed at ${skillDir}`, EXIT.filesystem, {
            path: skillDir,
            next: 'pass --force to overwrite'
          })
        }
        throw error
      }
      movedIntoPlace = true

      await store.upsertTarget(options.registry, options.namespace, options.slug, resolved.version, {
        agent: target.agent,
        rootDir: target.rootDir,
        installDir: skillDir,
        installedAt
      })
    } finally {
      if (!movedIntoPlace) {
        await rm(tempDir, { recursive: true, force: true }).catch(() => {})
      }
    }

    installed.push({ agent: target.agent, dir: skillDir })
  }

  return { installed }
}
