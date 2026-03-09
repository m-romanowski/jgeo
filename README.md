# jgeo

## Quickstart

- [Java 25+](https://www.oracle.com/java/technologies/downloads/)
- [Maven](https://maven.apache.org/download.cgi)

## Modules

### [jgeo-postal](./jgeo-postal)

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

### Features

| Feature | Description                                               |
|---------|-----------------------------------------------------------|
| In-memory lookup | Fast O(1) access for ZIP, city, and country               |
| Multi-dimensional queries | Lookup by ZIP, city, or country                           |
| Zero-copy slicing | Uses `ArraySliceList` to avoid temporary list allocations |
| Autocomplete | City name type-ahead using a Apache Lucene FST            |
| Compact memory | ~120 MB for the entire world dataset                      |
| Easy index rebuild | Automatically build `.bin.gz` index from GeoNames         |

### Dataset

Uses [GeoNames Postal Code Data](http://www.geonames.org/export/zip/) (`allCountries.zip`) which contains:

- Country code
- Postal code
- Place name (city)
- Admin name (state / province)

### Memory and Performance

- World dataset (~1.5M postal codes) in-memory: **15–20 MB**
- Lookup times: **~50 microseconds** for ZIP, city, or country
- Autocomplete: **~100 microseconds** for prefix search
- No large object allocations during pagination

## TODO
- Add index builder for [jgeo-postal](./jgeo-postal)

## License

This project uses **GeoNames postal dataset**, licensed under [CC BY 3.0](http://www.geonames.org/export/zip/).  
The code is **MIT licensed**.
