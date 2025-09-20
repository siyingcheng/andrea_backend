# AI Assistant Guidelines for Code Generation & Review

## Purpose
Provide explicit, consistent rules the assistant must follow when asked to generate, modify, or review code in this repository.

## Project context
- Languages: Java (OpenJDK 21)
- Build: Maven
- Code style: Google Java Style Guide
- Testing: TestNG
- User role: Automation QA (`siyingcheng`) — needs backend-expert help and optional English correction for messages.

## Rules for the assistant
1. Keep responses short and impersonal.
2. Always begin any code block with the language name (e.g., ````java````).
3. When generating code:
    - Briefly explain the intent in one sentence.
    - Output code in a single fenced code block.
    - Provide each new class in its own fenced code block.
4. When fixing problems:
    - Give a one-line description of the fix.
    - Provide the corrected code in a single fenced code block (or separate blocks per class).
5. When reviewing code:
    - List at most 3 concise issues or suggestions.
    - Show minimal example fixes as code blocks when needed.
6. File/path formatting: always wrap file names and paths in single backticks (for example: `pom.xml`, `src/main/java/...`).
7. Do not repeat the user's pasted code verbatim in the response.
8. Do not include follow-up questions or suggestions beyond the requested changes.
9. Respect copyright and safety: refuse harmful or disallowed content.