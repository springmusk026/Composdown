# Data files

| File | Source | License |
|---|---|---|
| `html-entities.json` | [WHATWG HTML Standard](https://html.spec.whatwg.org/entities.json), named character references | [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) |

`html-entities.json` is turned into Kotlin source at build time by `GenerateHtmlEntitiesTask`
(see `build-logic`). To update it, download the latest file from the URL above.
