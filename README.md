# climenu

A command-line interface should be inspectable as an interface, not memorized as folklore.

`climenu` turns CLI metadata into one browsable GUI: flags together, descriptions beside them, examples beside those, with provenance retained so `--help`, man pages, completion files, and argument-parser/source metadata can be merged instead of making the user visit several representations.

## First Edric slice

`Climenu.idric` implements the semantic core as a pure transformation:

```text
climenu : String → String → String
```

Give it an executable name and ordinary `--help` text; it returns a self-contained searchable HTML page.

The page currently provides:

- every option line the help parser recognizes;
- the documented option spelling and description;
- an automatically constructed example, preferring the long spelling;
- provenance (`--help` in this first adapter);
- filtering across all options.

`example.help` is a small parser fixture. `example.html` is the corresponding browser-viewable output fixture.

The desired shell interface is:

```sh
PROGRAM --help | climenu PROGRAM > PROGRAM.html
```

That process/stdin adapter is deliberately not claimed yet. Existing Edric code uses `System` for process arguments, but I did not find a demonstrated stdin-reading path in the current Edric repositories and could not compile one here. This PR keeps the semantic core independent of that unresolved runtime boundary rather than guessing at it.

## Internal model

The renderer consumes one normalized command description:

```text
Command
  executable
  summary
  flags

Flag
  syntax
  description
  examples
  origins
```

An `origin` says where a fact came from. The model already names four evidence sources:

```text
--help
man page
completion specification
argument-parser/source metadata
```

New discovery mechanisms should enrich this same description rather than create new user interfaces.

## Discovery

No single method works for every CLI. The intended evidence ladder is:

1. structured metadata deliberately exposed by the program;
2. argument-parser/source introspection;
3. shell-completion specifications;
4. man pages;
5. `--help` text as the broad fallback.

They are not mutually exclusive modes. `climenu` should merge what they know while retaining provenance: parser metadata may know that a value is an enum, completion data may know legal values, a man page may explain semantics better, and examples may provide realistic invocations.

## Next slices

- preserve wrapped/multiline help descriptions;
- split aliases and option arguments into typed fields;
- represent subcommands as a command tree;
- merge a man-page adapter with `--help` evidence;
- add completion adapters;
- add parser-specific adapters where they expose better structure;
- turn typed option values into GUI controls rather than documentation cards;
- add the executable/stdin bridge once the Edric runtime boundary is established.

The generated HTML is only the first renderer. The normalized command description is the durable part.
