# Batalha Naval — G01

Jogo de Batalha Naval para terminal, desenvolvido em Java como projeto da disciplina de Programação Orientada a Objetos (Mackenzie).

O jogador enfrenta uma CPU com inteligência configurável, em um tabuleiro e frota definidos por arquivo de propriedades. Todas as partidas são salvas em banco SQLite e podem ser listadas ou reproduzidas.

---

## Requisitos

| Ferramenta | Versão mínima |
|------------|---------------|
| JDK        | 21            |
| Maven      | 3.8+          |

---

## Estrutura do projeto

```
BatalhaNaval/
├── src/
│   ├── main/java/
│   │   ├── config/          # Enums e GameConfig (leitura do .properties)
│   │   ├── domain/          # Board, Ship, Fleet, Game, Coordinate, ShotResult
│   │   ├── persistence/     # DatabaseManager e repositórios SQLite
│   │   ├── player/          # Player, HumanPlayer, CpuPlayer
│   │   ├── replay/          # ReplayService (LIST e REPLAY)
│   │   ├── ui/              # TerminalUI
│   │   └── validation/      # FleetValidator, ValidationResult
│   └── test/java/           # Testes JUnit 5
├── data/                    # Banco SQLite gerado em runtime
├── game.properties          # Configuração da partida
├── pom.xml
└── sqlite-jdbc-3.36.0.3.jar
```

---

## Como compilar e rodar

**Compilar:**
```bash
mvn compile
```

**Rodar o jogo:**
```bash
java -cp "target\classes;sqlite-jdbc-3.36.0.3.jar" Main
```

**Rodar os testes:**
```bash
mvn test
```

> No Windows, se o Java 21 não for o padrão do sistema, defina-o antes de rodar:
> ```powershell
> $env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.11"
> $env:PATH = "C:\Program Files\Java\jdk-21.0.11\bin;$env:PATH"
> ```

---

## Modos de execução

Controlados pela propriedade `game.mode` no `game.properties`:

| Modo     | Comportamento |
|----------|---------------|
| `PLAY`   | Inicia uma partida normal contra a CPU |
| `LIST`   | Lista todas as partidas salvas no banco |
| `REPLAY` | Pede um ID e reproduz a partida movimento a movimento |

---

## Configuração (`game.properties`)

### Tabuleiro
| Propriedade | Exemplo | Descrição |
|-------------|---------|-----------|
| `board.size` | `10` | Tamanho do tabuleiro (N×N) |
| `board.columns.start` | `A` | Primeira coluna |
| `board.columns.end` | `J` | Última coluna |

### Frota
| Propriedade | Exemplo | Descrição |
|-------------|---------|-----------|
| `fleet.sizes` | `5,4,3,3,2` | Tamanho de cada navio |
| `fleet.names` | `Porta-avioes,...` | Nome de cada navio (mesma ordem) |
| `fleet.adjacency_rule` | `ORTHO_DIAG` | Regra de adjacência entre navios |

**Regras de adjacência:**
- `NONE` — navios podem se tocar
- `ORTHO` — navios não podem se tocar lateralmente
- `ORTHO_DIAG` — navios não podem se tocar nem lateralmente nem na diagonal

### Regras de turno
| Propriedade | Exemplo | Descrição |
|-------------|---------|-----------|
| `rules.hit_grants_extra_shot` | `false` | Acerto concede tiro extra |
| `rules.max_extra_shots` | `3` | Limite de tiros extras por turno |

### CPU
| Propriedade | Exemplo | Descrição |
|-------------|---------|-----------|
| `cpu.strategy` | `HUNT` | Estratégia da CPU |
| `cpu.use_parity_preference` | `true` | Prefere células (x+y) par |

**Estratégias da CPU:**
- `RANDOM` — atira aleatoriamente
- `HUNT` — aleatório até acertar; depois explora os vizinhos
- `PARITY` — prefere células de paridade par antes de caçar

### Persistência
| Propriedade | Exemplo | Descrição |
|-------------|---------|-----------|
| `db.enabled` | `true` | Habilita gravação no banco |
| `db.sqlite.file` | `data/batalha_naval.db` | Caminho do arquivo SQLite |
| `db.auto_migrate` | `true` | Cria tabelas automaticamente |
| `db.save_initial_fleet` | `true` | Salva posição inicial da frota |

### Reprodutibilidade
| Propriedade | Exemplo | Descrição |
|-------------|---------|-----------|
| `game.seed` | `42` | Seed fixa para posicionamento (vazio = aleatório) |

---

## Banco de dados

O arquivo SQLite é criado automaticamente em `data/batalha_naval.db` na primeira execução com `db.enabled=true`.

**Tabelas:**

| Tabela | Conteúdo |
|--------|----------|
| `matches` | Uma linha por partida (jogador, estratégia, vencedor, data) |
| `players` | Dois registros por partida (HUMAN e CPU) |
| `moves` | Um registro por tiro (turno, ator, coordenada, resultado) |
| `fleet_initial` | Posição inicial de cada navio (se `db.save_initial_fleet=true`) |

Para visualizar o banco, instale a extensão **SQLite Viewer** no VS Code e clique no arquivo `.db`.

---

## Legenda do tabuleiro

| Símbolo | Significado |
|---------|-------------|
| `.` | Célula vazia / desconhecida |
| `S` | Navio |
| `X` | Acerto |
| `o` | Água (tiro na água) |

---

## Arquitetura

O projeto segue separação em camadas:

- **`config`** — leitura de configuração e enums de regras
- **`domain`** — lógica central do jogo (Board, Ship, Fleet, Game)
- **`player`** — comportamento de jogadores (humano e CPU)
- **`ui`** — toda saída visual no terminal
- **`validation`** — validação do posicionamento de frota
- **`replay`** — listagem e reprodução de partidas salvas
- **`persistence`** — acesso ao banco SQLite

`Game` orquestra o loop de jogo sem depender de banco ou UI diretamente — recebe `TerminalUI` e `Scanner` por injeção, o que permite testes sem I/O.
