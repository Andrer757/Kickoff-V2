#!/usr/bin/env bash
# =============================================================================
# Kickoff AI Development Kit Setup
# setup-ai.sh — macOS / Linux
# =============================================================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SETUP_FAILED=0
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo ""
echo "=============================================="
echo "    Kickoff Android AI Development Kit Setup   "
echo "=============================================="
echo " Target Project: $PROJECT_ROOT"
echo ""

# ------------------------------------------------------------------------------
# Step 1: Check prerequisites
# ------------------------------------------------------------------------------
echo -e "${YELLOW}[1/5] Checking prerequisites...${NC}"

check_tool() {
  if command -v "$1" &> /dev/null; then
    echo -e "${GREEN}  ✓ $1 found${NC}"
  else
    echo -e "${YELLOW}  ⚠ $1 not found ($2)${NC}"
  fi
}

check_tool "git" "Required for version control"
check_tool "java" "Required for Android Gradle builds (JDK 21-23 recommended)"
check_tool "code" "VS Code CLI (run 'Shell Command: Install code command in PATH' from VS Code)"

# ------------------------------------------------------------------------------
# Step 2: Install Android CLI
# ------------------------------------------------------------------------------
echo ""
echo -e "${YELLOW}[2/5] Checking Android CLI...${NC}"
if command -v android &> /dev/null; then
  echo -e "${GREEN}  ✓ Android CLI already installed${NC}"
else
  echo "  Installing Android CLI..."
  if curl -fsSL https://dl.google.com/android/cli/latest/linux_x86_64/install.sh 2>/dev/null | bash; then
    echo -e "${GREEN}  ✓ Android CLI installed${NC}"
  else
    echo -e "${YELLOW}  ⚠ Could not auto-install Android CLI. You can install it manually from Google developer tools.${NC}"
  fi
fi

# ------------------------------------------------------------------------------
# Step 3: Optional Engram MCP (Local AI Memory)
# ------------------------------------------------------------------------------
WITH_ENGRAM="${KICKOFF_WITH_ENGRAM:-0}"
for arg in "$@"; do
  if [ "$arg" = "--with-engram" ]; then
    WITH_ENGRAM=1
  fi
done

echo ""
echo -e "${YELLOW}[3/5] Checking Engram MCP (Optional Local Context Memory)...${NC}"
if command -v engram &> /dev/null; then
  echo -e "${GREEN}  ✓ Engram already installed${NC}"
else
  SHOULD_INSTALL_ENGRAM=0
  if [ "$WITH_ENGRAM" = "1" ]; then
    SHOULD_INSTALL_ENGRAM=1
  elif [ -t 0 ]; then
    read -r -p "  Install optional Engram MCP via Homebrew? [y/N] " engram_reply
    case "$engram_reply" in
      [yY]|[yY][eE][sS]) SHOULD_INSTALL_ENGRAM=1 ;;
    esac
  fi

  if [ "$SHOULD_INSTALL_ENGRAM" = "1" ]; then
    if command -v brew &> /dev/null; then
      echo "  Installing Engram via Homebrew..."
      if brew tap gentleman-programming/tap && brew install gentleman-programming/tap/engram; then
        echo -e "${GREEN}  ✓ Engram installed${NC}"
      else
        echo -e "${YELLOW}  ⚠ Engram install failed (optional; continuing without Engram).${NC}"
      fi
    else
      echo -e "${YELLOW}  ⚠ Homebrew not found. Install engram manually if using MCP memory.${NC}"
    fi
  else
    echo -e "${YELLOW}  ⚠ Skipping optional Engram install (pass --with-engram or set KICKOFF_WITH_ENGRAM=1 to enable).${NC}"
  fi
fi

# ------------------------------------------------------------------------------
# Step 4: Install Graphify (Codebase Knowledge Graph)
# ------------------------------------------------------------------------------
echo ""
echo -e "${YELLOW}[4/5] Checking Graphify...${NC}"
if command -v graphify &> /dev/null; then
  echo -e "${GREEN}  ✓ Graphify already installed${NC}"
elif command -v uv &> /dev/null; then
  uv tool install "graphifyy==0.7.16" 2>/dev/null && echo -e "${GREEN}  ✓ Graphify installed via uv${NC}" || echo -e "${YELLOW}  ⚠ Graphify install skipped${NC}"
elif command -v pipx &> /dev/null; then
  pipx install "graphifyy==0.7.16" 2>/dev/null && echo -e "${GREEN}  ✓ Graphify installed via pipx${NC}" || echo -e "${YELLOW}  ⚠ Graphify install skipped${NC}"
elif command -v pip3 &> /dev/null; then
  pip3 install "graphifyy==0.7.16" 2>/dev/null && echo -e "${GREEN}  ✓ Graphify installed via pip3${NC}" || echo -e "${YELLOW}  ⚠ Graphify install skipped${NC}"
else
  echo -e "${YELLOW}  ⚠ Python package manager not found. Install graphify manually: uv tool install graphifyy${NC}"
fi

# ------------------------------------------------------------------------------
# Step 5: Verify AI Agents and Skills
# ------------------------------------------------------------------------------
echo ""
echo -e "${YELLOW}[5/5] Verifying AI configurations in project...${NC}"

GITHUB_AGENTS="$PROJECT_ROOT/.github/agents"
CLAUDE_AGENTS="$PROJECT_ROOT/.claude/agents"
SHARED_SKILLS="$PROJECT_ROOT/.agents/skills"

[ -d "$GITHUB_AGENTS" ] && echo -e "${GREEN}  ✓ GitHub Copilot Agents ready (${GITHUB_AGENTS})${NC}"
[ -d "$CLAUDE_AGENTS" ] && echo -e "${GREEN}  ✓ Claude Code Agents ready (${CLAUDE_AGENTS})${NC}"
[ -d "$SHARED_SKILLS" ] && echo -e "${GREEN}  ✓ Cursor / Antigravity Skills ready (${SHARED_SKILLS})${NC}"

echo ""
echo "=============================================="
if [ "$SETUP_FAILED" -eq 0 ]; then
  echo -e "${GREEN}  ✓ AI Development Kit ready!${NC}"
else
  echo -e "${RED}  ✗ AI Development Kit setup incomplete (see errors above)${NC}"
fi
echo "=============================================="
echo ""
echo "How to use with your preferred AI tool:"
echo ""
echo "  • GitHub Copilot:"
echo "      Open project in VS Code, open Copilot Chat, invoke '@AndroidArchitectAgent' or '@AndroidProgrammer'."
echo ""
echo "  • Claude Code:"
echo "      Run 'claude' in the project directory. Agents and skills load automatically from .claude/."
echo ""
echo "  • Cursor / Antigravity / Windsurf:"
echo "      Skills in .agents/skills/ and .claude/skills/ are auto-discovered."
echo ""

exit "$SETUP_FAILED"
