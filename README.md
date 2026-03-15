# jgeo

A **high-performance, in-memory postal code lookup** for multiple countries.

- ZIP → City / State lookup
- City → ZIP lookup with **pagination**
- Country → ZIP list with **pagination**
- City autocomplete with **prefix search**
- **Zero-copy array-backed pagination** for minimal memory usage
- **In-memory arrays** used for ZIPs, cities, and countries
- **Apache Lucene FST** used for fast city autocomplete
- **ArraySliceList** allows zero-copy pagination (no extra memory allocated per request)
- Suitable for **millions of ZIPs worldwide**

Built with **Java**, using **[GeoNames](https://www.geonames.org/) postal code dataset**.

## Quickstart

- [Java 25+](https://www.oracle.com/java/technologies/downloads/)
- [Maven](https://maven.apache.org/download.cgi)

## Modules

* [jgeo-postal](./jgeo-postal) - Core implementation
* [jgeo-postal-tools](./jgeo-postal-tools) - DEV toolkit

## Features

| Feature                   | Description                                                       |
|---------------------------|-------------------------------------------------------------------|
| In-memory lookup          | Fast O(log n) via binary search access for ZIP, city, and country |
| Multi-dimensional queries | Lookup by ZIP, city, or country                                   |
| Zero-copy slicing         | Uses `ArraySliceList` to avoid temporary list allocations         |
| Autocomplete              | City name type-ahead using a Apache Lucene FST                    |
| Compact memory            | ~140 MB for the entire world dataset                              |
| Easy index rebuild        | Automatically build `.bin.gz` index from GeoNames                 |

## Dataset

Uses [GeoNames Postal Code Data](http://www.geonames.org/export/zip/) (`allCountries.zip`) which contains:

- Country code
- Postal code
- Place name (city)
- Admin name (state / province)

The `GeoNamesIndexRunner` converts it into a **compressed `.bin.gz` binary index** for fast
in-memory loading - see [jgeo-postal-tools](./jgeo-postal-tools)

## Memory and Performance

- World dataset (~1.8M postal codes) in-memory: **~140 MB**
- Lookup times: **~50 microseconds** for ZIP, city, or country
- Autocomplete: **~100 microseconds** for prefix search
- No large object allocations during pagination

## Rebuilding the Index

To refresh data:

1. Delete old `postal-index.bin.gz`
2. Run `GeoNamesIndexRunner`

## Usage

```bash
java GeoNamesIndexRunner [OPTIONS]
```

## Options

| Option       | Argument  | Description                                                                                                                        |
|--------------|-----------|------------------------------------------------------------------------------------------------------------------------------------|
| `--url`      | `<url>`   | URL to download the GeoNames data file from                                                                                        |
| `--data-dir` | `<path>`  | Local directory where downloaded data will be stored                                                                               |
| `--index`    | `<path>`  | Output path for the generated index file                                                                                           |
| `--country`  | `<codes>` | Comma-separated ISO 3166-1 alpha-2 country codes or predefined constants, e.g. EU, to include (case-insensitive). Can be repeated. |

All options are optional - defaults are provided by `GeoNamesIndexBuilder`.

## Examples

### Build a full index with default settings

```bash
java GeoNamesIndexRunner
```

### Build an index for specific countries

```bash
java GeoNamesIndexRunner \
  --url https://download.geonames.org/export/zip/allCountries.zip \
  --data-dir ./data \
  --index ./geonames.index \
  --country PL,DE,FR
```

### Specify countries with repeated flags

```bash
java GeoNamesIndexRunner \
  --country PL \
  --country DE \
  --country FR
```

Both forms can be mixed. Country codes are case-insensitive (`pl`, `PL`, and `Pl` are all valid).

## Notes

- The `--country` filter is optional. If omitted, all countries are indexed.
- You can use `--country EU` to build index for all countries in Europe.

## License

This project uses **GeoNames postal dataset**, licensed under [CC BY 3.0](http://www.geonames.org/export/zip/).  
The code is **MIT licensed**.
