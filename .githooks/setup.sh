#!/usr/bin/env bash
# Setup script to configure Git to use .githooks directory
# Run this once after cloning the repository

echo "🔧 Configuring Git hooks..."
echo ""

# Configure Git to use .githooks directory instead of .git/hooks
git config core.hooksPath .githooks

echo "✅ Git hooks configured successfully!"
echo ""
echo "Git will now use hooks from .githooks/ directory."
echo "This applies to all worktrees and new branches automatically."
echo ""
echo "Hooks enabled:"
echo "  - pre-commit: Code formatting check (Spotless) + governance reminder"
echo "  - post-commit: Async task reminders (Linear updates, PR checks, tests, docs)"
echo ""
