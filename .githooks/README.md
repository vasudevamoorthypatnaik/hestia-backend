# Git Hooks for Hestia Backend

This directory contains Git hooks for maintaining code quality and governance compliance.

## Setup (One-Time)

After cloning the repository, run:

```bash
.githooks/setup.sh
```

This configures Git to use hooks from this directory instead of `.git/hooks/`.

## Hooks

### `pre-commit` (Blocking)

Runs before each commit to ensure:
- **Code formatting** - Validates code formatting using Spotless (`mvn spotless:check`)
- **Governance reminder** - Reminds about updating governance docs

**If formatting fails:**

```bash
mvn spotless:apply  # Auto-format code
git add .            # Stage formatted files
git commit          # Try again
```

### `pre-commit-governance-check` (Non-Blocking)

Reminder for governance documentation updates:
- Add governance learnings to `.cursor/rules/*.mdc` files
- Document new conventions or architecture decisions

### `post-commit` (Non-Blocking)

Runs after each successful commit to remind Claude Code about async tasks:

1. **Update Linear ticket** - Add commit description to relevant Linear ticket
2. **Check PR mergeability** - Verify PR is in mergeable state, resolve conflicts if needed
3. **Verify tests** - Run unit tests (and integration tests if needed) to ensure commit is stable
4. **Update domain READMEs** - Document changes in domain-specific README files:
   - `hestia-user/src/main/java/com/hestia/user/README.md`
   - `hestia-event/src/main/java/com/hestia/event/README.md`
   - `hestia-invitation/src/main/java/com/hestia/invitation/README.md`

**Purpose:** Ensures follow-up tasks are completed before marking work as done.

## How It Works

**Git configuration:**

```bash
git config core.hooksPath .githooks
```

This tells Git to use `.githooks/` for all hooks instead of `.git/hooks/`.

**Works with:**
- ✅ Main repository
- ✅ All worktrees (`.claude/worktrees/*`)
- ✅ New branches (hooks apply automatically)

## Bypassing Hooks (Emergency Only)

To skip hooks temporarily (not recommended):

```bash
git commit --no-verify -m "message"
```

**⚠️ Warning:** Only use `--no-verify` in emergencies. Bypassing hooks can lead to unformatted code or missing documentation.

## Benefits

1. **Consistent code formatting** - All commits follow the same style (Google Java Format with AOSP)
2. **Shared configuration** - Hooks are tracked in Git, everyone gets the same setup
3. **Worktree-friendly** - Works seamlessly with Git worktrees
4. **Native Git** - No Node.js, Maven plugins, or external dependencies required
5. **Documentation reminders** - Helps maintain high-quality domain documentation

## Troubleshooting

### Hooks not running

Verify configuration:

```bash
git config core.hooksPath
```

Should output: `.githooks`

If not set, run setup again:

```bash
.githooks/setup.sh
```

### Permission denied errors

Make hooks executable:

```bash
chmod +x .githooks/pre-commit .githooks/pre-commit-governance-check .githooks/post-commit
```

### Spotless check fails

Auto-format your code:

```bash
mvn spotless:apply
```

Then stage and commit again.
