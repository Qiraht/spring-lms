# Coding Standard (WIP)

## 1. Naming Conventions

### 1.1 File and Folder

### 1.2 Code-Level
| Items                        | Conventions          | example     |
|------------------------------|----------------------|-------------|
| Reference Types*             | PascalCase           | ApiResponse |
| Data Types (Entities / DTOs) | PascalCase           | User        |
| Functions / Methods          | camelCase            | getUserById |
| Variables                    | camelCase            | newUser     |
| Constants                    | SCREAMING_SNAKE_CASE | CACHE_KEY   |

*Reference Types: Record, Class, Interface, Enums



Use "fetchType.Lazy" on One-to-Many and Many-to-Many
Use LF space

## Git Workflows

### Git Branch

This branch uses git flow branching model. It's recommended to use `git flow` feature

#### Core branch
1. main, store production ready code
2. develop, Integration branch between completed features and production ready code

### Supporting branch
1. feature, created for developing new feature (including refactor and fix)
2. hotfix, used to quickly address critical bugs in production/main branch 
3. release, used to prepare for a new production release

Naming conventions for Supporting Branch
1. feature, `feature/name-of-feature`
2. hotfix, `hotfix/critical-bug`
3. release, `release/tag-number`

### Commit Rules

Follow Conventional Commits
```
<type>(<scopes>): <messages>

[optional body]
```

Rules:
- Limit subject line to 72 character
- Use [git commit conventional](https://www.conventionalcommits.org/en/v1.0.0/) message, eg: feat, fix, chore, refactor, docs, etc.
- Use the imperative mood in the subject line
- Do not end the subject line with a period
- Separate subject from body with a blank line
- AI Co-Authored is prohibited. This decision is taken as the developer is responsible and held accountability for AI Usage for development
- Always run `mvn spotless:apply` before commit to
- (If needed) Use the body to explain what and why vs. how


Useful resource;
- [Chris Beams: How to write git commit](https://chris.beams.io/git-commit)
- [Conventional Commit](https://www.conventionalcommits.org/en/v1.0.0/)


## Code Formatting
Formatting uses Palantir Java Format via Spotless Maven. 
1. use `mvn spotless:check` to check for any violations
2. use `mvn spotless:apply` to apply code formatting