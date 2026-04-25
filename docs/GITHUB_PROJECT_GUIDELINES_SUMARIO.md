# Diretrizes (Resumo: Branches e Commits)

Este arquivo contém as regras essenciais sobre branching e convenção de commits a serem seguidas neste repositório.

1. Branching model

- main: branch protegida com o estado pronto para produção. Nunca commitar direto.
- feature/<JIRA-123>-<descricao>: branches de trabalho para novas features e correções. Incluir a chave do ticket quando aplicável.
- release/<versao>: preparar release e ajustes finais; merge para main.
- hotfix/<JIRA-123>-<descricao>: correções urgentes a partir de main.

Regras práticas:
- Criar branch a partir de main (ou develop se houver uma política local).
- Nome curto, kebab-case, incluir referência ao ticket: feature/JIRA-123-login-oauth.
- Manter branches pequenos e focados; preferir PRs menores e iterativos.

2. Convenção de commits

Recomendado: Conventional Commits simplificado.
Formato: <tipo>(escopo opcional): <resumo>

Tipos sugeridos:
- feat: nova funcionalidade
- fix: correção de bug
- docs: documentação
- refactor: refatoração
- test: testes
- chore: manutenção

Exemplos:
- feat(auth): adicionar logout
- fix(api): corrigir NPE em /users

Boas práticas:
- Resumo conciso (<=72 caracteres);
- Incluir chave do ticket no resumo ou no corpo (ex: "JIRA-123: feat(auth): adicionar logout");
- Usar "Closes JIRA-123" ou "Refs #123" no corpo quando resolver o ticket.

3. Alternativa simplificada (trunk-based / Jira-first)

Se a equipe usar Jira como fonte de verdade e preferir um fluxo mais leve:
- Não é obrigatório abrir GitHub Issues; usar a chave do Jira nas branches e PRs.
- Fluxo: feature/* -> PR -> main (sem develop)
- Compromisso mínimo: prefira commits com prefixo simples e referência ao ticket.

Referências

- Conventional Commits: https://www.conventionalcommits.org/
- Semantic Versioning: https://semver.org/
- GitHub Docs - Branches and merging: https://docs.github.com/en/get-started/using-git/about-branches

---

Mantenha este arquivo curto e referencie o README principal para exceções ou decisões do time.