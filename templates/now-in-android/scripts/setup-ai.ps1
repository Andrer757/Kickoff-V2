# =============================================================================
# Kickoff AI Development Kit Setup
# setup-ai.ps1 — Windows PowerShell
# =============================================================================

$ErrorActionPreference = "Continue"

Write-Host ""
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "    Kickoff Android AI Development Kit Setup   " -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host ""

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir

Write-Host "Target Project: $ProjectRoot"
Write-Host ""

# ------------------------------------------------------------------------------
# Step 1: Check prerequisites
# ------------------------------------------------------------------------------
Write-Host "[1/5] Checking prerequisites..." -ForegroundColor Yellow

function Check-Tool ($command, $description) {
    if (Get-Command $command -ErrorAction SilentlyContinue) {
        Write-Host "  [OK] $command found" -ForegroundColor Green
    } else {
        Write-Host "  [WARN] $command not found ($description)" -ForegroundColor Yellow
    }
}

Check-Tool "git" "Required for version control"
Check-Tool "java" "Required for Android Gradle builds (Java 17/21 recommended)"
Check-Tool "code" "VS Code CLI"

# ------------------------------------------------------------------------------
# Step 2: Check Android CLI
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "[2/5] Checking Android CLI..." -ForegroundColor Yellow
if (Get-Command "android" -ErrorAction SilentlyContinue) {
    Write-Host "  [OK] Android CLI already installed" -ForegroundColor Green
} else {
    Write-Host "  [INFO] Android CLI not found in PATH." -ForegroundColor Gray
    Write-Host "         Download from official Google Android developer tools." -ForegroundColor Gray
}

# ------------------------------------------------------------------------------
# Step 3: Check Engram MCP
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "[3/5] Checking Engram MCP (Local Context Memory)..." -ForegroundColor Yellow
if (Get-Command "engram" -ErrorAction SilentlyContinue) {
    Write-Host "  [OK] Engram already installed" -ForegroundColor Green
} elseif (Get-Command "winget" -ErrorAction SilentlyContinue) {
    Write-Host "  [INFO] You can install engram via winget or scoop if using local MCP memory." -ForegroundColor Gray
}

# ------------------------------------------------------------------------------
# Step 4: Check Graphify
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "[4/5] Checking Graphify (Codebase Knowledge Graph)..." -ForegroundColor Yellow
if (Get-Command "graphify" -ErrorAction SilentlyContinue) {
    Write-Host "  [OK] Graphify already installed" -ForegroundColor Green
} elseif (Get-Command "uv" -ErrorAction SilentlyContinue) {
    Write-Host "  Installing Graphify via uv..."
    uv tool install "graphifyy==0.7.16"
} elseif (Get-Command "pip" -ErrorAction SilentlyContinue) {
    Write-Host "  Installing Graphify via pip..."
    pip install "graphifyy==0.7.16"
} else {
    Write-Host "  [WARN] Python package manager not found. Install graphify manually: pip install graphifyy" -ForegroundColor Yellow
}

# ------------------------------------------------------------------------------
# Step 5: Verify AI Agents and Skills
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "[5/5] Verifying AI configurations in project..." -ForegroundColor Yellow

$GithubAgents = Join-Path $ProjectRoot ".github\agents"
$ClaudeAgents = Join-Path $ProjectRoot ".claude\agents"
$SharedSkills = Join-Path $ProjectRoot ".agents\skills"

if (Test-Path $GithubAgents) { Write-Host "  [OK] GitHub Copilot Agents ready ($GithubAgents)" -ForegroundColor Green }
if (Test-Path $ClaudeAgents) { Write-Host "  [OK] Claude Code Agents ready ($ClaudeAgents)" -ForegroundColor Green }
if (Test-Path $SharedSkills) { Write-Host "  [OK] Cursor / Antigravity Skills ready ($SharedSkills)" -ForegroundColor Green }

Write-Host ""
Write-Host "==============================================" -ForegroundColor Green
Write-Host "  AI Development Kit ready!                   " -ForegroundColor Green
Write-Host "==============================================" -ForegroundColor Green
Write-Host ""
Write-Host "How to use with your preferred AI tool:"
Write-Host "  - GitHub Copilot: Invoke '@AndroidArchitectAgent' or '@AndroidProgrammer' in Copilot Chat."
Write-Host "  - Claude Code: Run 'claude' in project directory (.claude/ loaded automatically)."
Write-Host "  - Cursor / Antigravity: Skills in .agents/skills/ and .claude/skills/ auto-discovered."
Write-Host ""
