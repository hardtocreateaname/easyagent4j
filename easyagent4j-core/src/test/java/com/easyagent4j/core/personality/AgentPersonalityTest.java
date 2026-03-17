package com.easyagent4j.core.personality;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentPersonality的单元测试。
 */
class AgentPersonalityTest {

    @Test
    void builder_shouldCreateEmptyPersonality() {
        AgentPersonality personality = AgentPersonality.builder().build();

        assertNull(personality.getName());
        assertNull(personality.getRole());
        assertNull(personality.getTone());
        assertTrue(personality.getTraits().isEmpty());
        assertNull(personality.getResponseStyle());
        assertTrue(personality.getBoundaries().isEmpty());
        assertTrue(personality.getPrinciples().isEmpty());
    }

    @Test
    void builder_shouldSetAllFields() {
        AgentPersonality personality = AgentPersonality.builder()
                .name("Claude")
                .role("AI Assistant")
                .tone("Professional and friendly")
                .traits(List.of("Helpful", "Precise", "Polite"))
                .responseStyle("Concise and informative")
                .boundaries(List.of("No harmful content", "No medical advice"))
                .principles(List.of("Be helpful", "Be honest", "Be safe"))
                .build();

        assertEquals("Claude", personality.getName());
        assertEquals("AI Assistant", personality.getRole());
        assertEquals("Professional and friendly", personality.getTone());
        assertEquals(List.of("Helpful", "Precise", "Polite"), personality.getTraits());
        assertEquals("Concise and informative", personality.getResponseStyle());
        assertEquals(List.of("No harmful content", "No medical advice"), personality.getBoundaries());
        assertEquals(List.of("Be helpful", "Be honest", "Be safe"), personality.getPrinciples());
    }

    @Test
    void builder_shouldSupportChaining() {
        AgentPersonality personality = AgentPersonality.builder()
                .name("Test")
                .role("Tester")
                .addTrait("Trait1")
                .addTrait("Trait2")
                .addBoundary("Boundary1")
                .addPrinciple("Principle1")
                .build();

        assertEquals("Test", personality.getName());
        assertEquals("Tester", personality.getRole());
        assertEquals(List.of("Trait1", "Trait2"), personality.getTraits());
        assertEquals(List.of("Boundary1"), personality.getBoundaries());
        assertEquals(List.of("Principle1"), personality.getPrinciples());
    }

    @Test
    void builder_shouldCreateImmutableLists() {
        List<String> originalTraits = List.of("Trait1", "Trait2");
        AgentPersonality personality = AgentPersonality.builder()
                .traits(originalTraits)
                .build();

        // 修改原始列表不应影响personality
        List<String> personalityTraits = personality.getTraits();
        personalityTraits.add("NewTrait");

        assertEquals(2, personality.getTraits().size());
        assertEquals(List.of("Trait1", "Trait2"), personality.getTraits());
    }

    @Test
    void buildSystemPrompt_shouldGeneratePromptWithAllFields() {
        AgentPersonality personality = AgentPersonality.builder()
                .name("Claude")
                .role("AI Assistant")
                .tone("Professional")
                .traits(List.of("Helpful", "Precise"))
                .responseStyle("Concise")
                .principles(List.of("Be honest", "Be safe"))
                .boundaries(List.of("No harmful content"))
                .build();

        String prompt = personality.buildSystemPrompt();

        assertTrue(prompt.contains("You are AI Assistant, named Claude."));
        assertTrue(prompt.contains("Tone: Professional"));
        assertTrue(prompt.contains("Traits:"));
        assertTrue(prompt.contains("- Helpful"));
        assertTrue(prompt.contains("- Precise"));
        assertTrue(prompt.contains("Response Style: Concise"));
        assertTrue(prompt.contains("Principles:"));
        assertTrue(prompt.contains("- Be honest"));
        assertTrue(prompt.contains("- Be safe"));
        assertTrue(prompt.contains("Boundaries:"));
        assertTrue(prompt.contains("- No harmful content"));
    }

    @Test
    void buildSystemPrompt_shouldHandleEmptyFields() {
        AgentPersonality personality = AgentPersonality.builder()
                .role("Assistant")
                .build();

        String prompt = personality.buildSystemPrompt();

        assertTrue(prompt.contains("You are Assistant"));
        assertFalse(prompt.contains("Tone:"));
        assertFalse(prompt.contains("Traits:"));
        assertFalse(prompt.contains("Response Style:"));
        assertFalse(prompt.contains("Principles:"));
        assertFalse(prompt.contains("Boundaries:"));
    }

    @Test
    void buildSystemPrompt_shouldHandleNameOnly() {
        AgentPersonality personality = AgentPersonality.builder()
                .name("TestBot")
                .build();

        String prompt = personality.buildSystemPrompt();

        assertFalse(prompt.contains("You are"));
        assertFalse(prompt.contains("named TestBot"));
    }

    @Test
    void fromJson_shouldParseValidJson() throws Exception {
        String json = """
            {
                "name": "Claude",
                "role": "AI Assistant",
                "tone": "Professional",
                "traits": ["Helpful", "Precise"],
                "responseStyle": "Concise",
                "principles": ["Be honest"],
                "boundaries": ["No harmful content"]
            }
            """;

        AgentPersonality personality = AgentPersonality.fromJson(json);

        assertEquals("Claude", personality.getName());
        assertEquals("AI Assistant", personality.getRole());
        assertEquals("Professional", personality.getTone());
        assertEquals(List.of("Helpful", "Precise"), personality.getTraits());
        assertEquals("Concise", personality.getResponseStyle());
        assertEquals(List.of("Be honest"), personality.getPrinciples());
        assertEquals(List.of("No harmful content"), personality.getBoundaries());
    }

    @Test
    void fromJson_shouldParsePartialJson() throws Exception {
        String json = """
            {
                "name": "Claude",
                "role": "AI Assistant"
            }
            """;

        AgentPersonality personality = AgentPersonality.fromJson(json);

        assertEquals("Claude", personality.getName());
        assertEquals("AI Assistant", personality.getRole());
        assertNull(personality.getTone());
        assertTrue(personality.getTraits().isEmpty());
        assertNull(personality.getResponseStyle());
        assertTrue(personality.getPrinciples().isEmpty());
        assertTrue(personality.getBoundaries().isEmpty());
    }

    @Test
    void fromJson_shouldThrowExceptionOnInvalidJson() {
        String invalidJson = "{ invalid json }";

        assertThrows(Exception.class, () -> AgentPersonality.fromJson(invalidJson));
    }

    @Test
    void fromMarkdown_shouldParseFullMarkdown() {
        String markdown = """
            ## Name
            Claude

            ## Role
            AI Assistant

            ## Tone
            Professional and friendly

            ## Traits
            - Helpful
            - Precise
            - Polite

            ## Response Style
            Concise and informative

            ## Principles
            - Be helpful
            - Be honest
            - Be safe

            ## Boundaries
            - No harmful content
            - No medical advice
            """;

        AgentPersonality personality = AgentPersonality.fromMarkdown(markdown);

        assertEquals("Claude", personality.getName());
        assertEquals("AI Assistant", personality.getRole());
        assertEquals("Professional and friendly", personality.getTone());
        assertEquals(List.of("Helpful", "Precise", "Polite"), personality.getTraits());
        assertEquals("Concise and informative", personality.getResponseStyle());
        assertEquals(List.of("Be helpful", "Be honest", "Be safe"), personality.getPrinciples());
        assertEquals(List.of("No harmful content", "No medical advice"), personality.getBoundaries());
    }

    @Test
    void fromMarkdown_shouldParsePartialMarkdown() {
        String markdown = """
            ## Role
            AI Assistant

            ## Traits
            - Helpful
            - Precise
            """;

        AgentPersonality personality = AgentPersonality.fromMarkdown(markdown);

        assertNull(personality.getName());
        assertEquals("AI Assistant", personality.getRole());
        assertNull(personality.getTone());
        assertEquals(List.of("Helpful", "Precise"), personality.getTraits());
        assertNull(personality.getResponseStyle());
        assertTrue(personality.getPrinciples().isEmpty());
        assertTrue(personality.getBoundaries().isEmpty());
    }

    @Test
    void fromMarkdown_shouldHandleCaseInsensitiveSections() {
        String markdown = """
            ## ROLE
            AI Assistant

            ## response style
            Concise

            ## TRAITS
            - Helpful
            """;

        AgentPersonality personality = AgentPersonality.fromMarkdown(markdown);

        assertEquals("AI Assistant", personality.getRole());
        assertEquals("Concise", personality.getResponseStyle());
        assertEquals(List.of("Helpful"), personality.getTraits());
    }

    @Test
    void fromMarkdown_shouldHandleEmptyLists() {
        String markdown = """
            ## Role
            AI Assistant

            ## Traits
            ## Principles
            """;

        AgentPersonality personality = AgentPersonality.fromMarkdown(markdown);

        assertEquals("AI Assistant", personality.getRole());
        assertTrue(personality.getTraits().isEmpty());
        assertTrue(personality.getPrinciples().isEmpty());
    }

    @Test
    void fromMarkdown_shouldHandleWhitespaceInLists() {
        String markdown = """
            ## Traits
            -   Helpful  
            -  Precise
              """;

        AgentPersonality personality = AgentPersonality.fromMarkdown(markdown);

        assertEquals(List.of("Helpful", "Precise"), personality.getTraits());
    }

    @Test
    void fromMarkdown_shouldHandleEmptyMarkdown() {
        String markdown = "";

        AgentPersonality personality = AgentPersonality.fromMarkdown(markdown);

        assertNull(personality.getName());
        assertNull(personality.getRole());
        assertNull(personality.getTone());
        assertTrue(personality.getTraits().isEmpty());
        assertNull(personality.getResponseStyle());
        assertTrue(personality.getBoundaries().isEmpty());
        assertTrue(personality.getPrinciples().isEmpty());
    }

    @Test
    void personalityLoader_shouldLoadJsonFile() throws IOException {
        Path tempFile = Files.createTempFile("personality", ".json");
        Files.writeString(tempFile, """
            {
                "name": "Test",
                "role": "Assistant"
            }
            """);

        try {
            AgentPersonality personality = PersonalityLoader.load(tempFile);
            assertEquals("Test", personality.getName());
            assertEquals("Assistant", personality.getRole());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void personalityLoader_shouldLoadMarkdownFile() throws IOException {
        Path tempFile = Files.createTempFile("personality", ".md");
        Files.writeString(tempFile, """
            ## Role
            Assistant

            ## Traits
            - Helpful
            """);

        try {
            AgentPersonality personality = PersonalityLoader.load(tempFile);
            assertEquals("Assistant", personality.getRole());
            assertEquals(List.of("Helpful"), personality.getTraits());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void personalityLoader_shouldThrowExceptionOnUnsupportedFormat() throws IOException {
        Path tempFile = Files.createTempFile("personality", ".txt");
        Files.writeString(tempFile, "test");

        try {
            assertThrows(IOException.class, () -> PersonalityLoader.load(tempFile));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void personalityLoader_shouldThrowExceptionOnInvalidJson() throws IOException {
        Path tempFile = Files.createTempFile("personality", ".json");
        Files.writeString(tempFile, "{ invalid }");

        try {
            assertThrows(IOException.class, () -> PersonalityLoader.load(tempFile));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void buildSystemPrompt_shouldNotHaveTrailingWhitespace() {
        AgentPersonality personality = AgentPersonality.builder()
                .role("Assistant")
                .traits(List.of("Helpful"))
                .principles(List.of("Be honest"))
                .build();

        String prompt = personality.buildSystemPrompt();

        // 不应该有连续的空行
        assertFalse(prompt.contains("\n\n\n"));
    }
}
