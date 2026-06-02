#!/usr/bin/env bash
#
# Futbix Setup Hooks
# ==================
# Configures git hooks and installs tooling dependencies.
# Run once after cloning the repository.

set -e

CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'
BOLD='\033[1m'

echo ""
echo "${CYAN}${BOLD}⚙  Futbix Project Setup${NC}"
echo "${CYAN}━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# ── Git hooks ──────────────────────────────────────────────────────────

echo "${CYAN}🔗 Installing git hooks...${NC}"
git config core.hooksPath .githooks
chmod +x .githooks/pre-commit .githooks/commit-msg
echo "${GREEN}✓  Git hooks installed at .githooks/${NC}"
echo ""

# ── ESLint + Prettier (frontend) ───────────────────────────────────────

if [ -d "frontend" ]; then
    echo "${CYAN}📦 Setting up frontend tooling...${NC}"
    cd frontend

    if [ ! -d "node_modules" ]; then
        echo "   Installing npm dependencies..."
        npm install
    fi

    if [ ! -f "eslint.config.js" ]; then
        echo "   Installing ESLint + Prettier..."
        npm install --save-dev \
            eslint@9 \
            @eslint/js \
            eslint-plugin-react@latest \
            eslint-plugin-react-hooks@latest \
            prettier \
            eslint-config-prettier 2>/dev/null

        cat > eslint.config.js << 'ESLINT_EOF'
import js from "@eslint/js";
import reactPlugin from "eslint-plugin-react";
import reactHooks from "eslint-plugin-react-hooks";

export default [
  js.configs.recommended,
  reactPlugin.configs.flat.recommended,
  {
    plugins: { "react-hooks": reactHooks },
    rules: {
      "react/react-in-jsx-scope": "off",
      "react/prop-types": "warn",
      "no-console": ["warn", { allow: ["warn", "error"] }],
      "no-unused-vars": ["warn", { argsIgnorePattern: "^_" }],
      ...reactHooks.configs.recommended.rules,
    },
    settings: {
      react: { version: "detect" },
    },
  },
];
ESLINT_EOF

        cat > .prettierrc << 'PRETTIER_EOF'
{
  "semi": false,
  "singleQuote": true,
  "trailingComma": "all",
  "tabWidth": 2,
  "printWidth": 100
}
PRETTIER_EOF

        echo "${GREEN}✓  ESLint + Prettier configured${NC}"
    fi

    cd ..
fi

echo ""
echo "${GREEN}${BOLD}✓  Setup complete. You're ready to develop!${NC}"
echo ""
echo "${CYAN}   Available commands:${NC}"
echo "   cd frontend && npm run lint     Check frontend code"
echo "   cd frontend && npm run format   Format frontend code"
echo "   mvn test                        Run backend tests"
echo ""
