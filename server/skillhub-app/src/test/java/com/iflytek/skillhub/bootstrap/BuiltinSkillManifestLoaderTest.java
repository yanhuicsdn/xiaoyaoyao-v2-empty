package com.iflytek.skillhub.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ResourceLoader;

import java.nio.charset.StandardCharsets;
import java.util.List;

class BuiltinSkillManifestLoaderTest {

    @Test
    void loadsManifestItemsInOrder() {
        BuiltinSkillManifestLoader loader = loaderWith("""
                {
                  "skills": [
                    {"slug": "skillhub-hello", "version": "1.0.0", "url": "https://bjcdn.openstorage.cn/skillhub-hello.zip"},
                    {"slug": "skillhub-hello", "version": "1.1.0", "url": "https://cdn.bjcdn.openstorage.cn/skillhub-hello.zip"}
                  ]
                }
                """);

        List<BuiltinSkillManifestLoader.ManifestItem> items = loader.load();

        assertThat(items)
                .extracting(BuiltinSkillManifestLoader.ManifestItem::version)
                .containsExactly("1.0.0", "1.1.0");
    }

    @Test
    void returnsEmptyListWhenManifestIsMissing() {
        BuiltinSkillManifestLoader loader = new BuiltinSkillManifestLoader(
                new ObjectMapper(),
                new ResourceLoader() {
                    @Override
                    public org.springframework.core.io.Resource getResource(String location) {
                        return new MissingResource();
                    }

                    @Override
                    public ClassLoader getClassLoader() {
                        return getClass().getClassLoader();
                    }
                }
        );

        assertThat(loader.load()).isEmpty();
    }

    @Test
    void returnsEmptyListWhenManifestIsMalformed() {
        BuiltinSkillManifestLoader loader = loaderWith("{not-json");

        assertThat(loader.load()).isEmpty();
    }

    @Test
    void returnsEmptyListWhenManifestIsEmpty() {
        BuiltinSkillManifestLoader loader = loaderWith("");

        assertThat(loader.load()).isEmpty();
    }

    @Test
    void skipsItemsWithMissingHumanFieldsAndDuplicateSlugVersion() {
        BuiltinSkillManifestLoader loader = loaderWith("""
                {
                  "skills": [
                    {"slug": "skillhub-hello", "version": "1.0.0", "url": "https://bjcdn.openstorage.cn/first.zip"},
                    {"slug": "skillhub-hello", "version": "1.0.0", "url": "https://bjcdn.openstorage.cn/second.zip"},
                    {"slug": "InvalidUppercase", "version": "1.0.0", "url": "https://bjcdn.openstorage.cn/invalid.zip"},
                    {"slug": "", "version": "1.0.0", "url": "https://bjcdn.openstorage.cn/blank.zip"},
                    {"slug": "missing-version", "url": "https://bjcdn.openstorage.cn/missing-version.zip"},
                    {"slug": "missing-url", "version": "1.0.0"},
                    {"slug": "valid-after-invalid", "version": "1.0.0", "url": "https://bjcdn.openstorage.cn/valid.zip"}
                  ]
                }
                """);

        List<BuiltinSkillManifestLoader.ManifestItem> items = loader.load();

        assertThat(items)
                .extracting(BuiltinSkillManifestLoader.ManifestItem::url)
                .containsExactly(
                        "https://bjcdn.openstorage.cn/first.zip",
                        "https://bjcdn.openstorage.cn/valid.zip"
                );
    }

    @Test
    void capsManifestEntriesAtOneHundredRawEntries() {
        StringBuilder json = new StringBuilder("{\"skills\":[");
        for (int i = 0; i < 101; i++) {
            if (i > 0) {
                json.append(',');
            }
            if (i == 0) {
                json.append("{\"slug\":\"\",\"version\":\"1.0.0\",\"url\":\"https://bjcdn.openstorage.cn/blank.zip\"}");
            } else {
                json.append("{\"slug\":\"skill-").append(i)
                        .append("\",\"version\":\"1.0.0\",\"url\":\"https://bjcdn.openstorage.cn/skill-")
                        .append(i)
                        .append(".zip\"}");
            }
        }
        json.append("]}");

        BuiltinSkillManifestLoader loader = loaderWith(json.toString());

        assertThat(loader.load()).hasSize(99);
    }

    private BuiltinSkillManifestLoader loaderWith(String content) {
        ResourceLoader resourceLoader = new ResourceLoader() {
            @Override
            public org.springframework.core.io.Resource getResource(String location) {
                return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)) {
                    @Override
                    public boolean exists() {
                        return true;
                    }

                    @Override
                    public String getDescription() {
                        return "test manifest";
                    }
                };
            }

            @Override
            public ClassLoader getClassLoader() {
                return getClass().getClassLoader();
            }
        };
        return new BuiltinSkillManifestLoader(new ObjectMapper(), resourceLoader);
    }

    static class MissingResource extends ByteArrayResource {

        MissingResource() {
            super(new byte[0]);
        }

        @Override
        public boolean exists() {
            return false;
        }

        @Override
        public String getDescription() {
            return "missing manifest";
        }
    }
}
