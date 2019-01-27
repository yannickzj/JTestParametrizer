# Open Data
We present the results at each step of benchmark processing.

## Selected case study projects

| Project            | Location |
|:-------------------|:-----------|
| JFreeChart         | original/jfreechart-1.0.10-original.tar.gz |
| Gson               | original/gson-2.8.5-original.tar.gz |
| Apache Commons Lang| original/commons-lang-3.7-original.tar.gz |
| Apache Commons IO  | original/commons-io-2.5-original.tar.gz |
| Joda-Time          | original/joda-time-2.10-original.tar.gz |

## Clone detection (Deckard)

Clone detection reports from *Deckard* for each case study project:

| Project            | Location   |
|:-------------------|:-----------|
| JFreeChart         | detection/jfree_cluster_vdb_50_0_allg_0.95_50 | 
| Gson               | detection/gson_cluster_vdb_50_0_allg_0.95_50 | 
| Apache Commons Lang| detection/commons_lang_cluster_vdb_50_0_allg_0.95_50 | 
| Apache Commons IO  | detection/commons_io_cluster_vdb_50_0_allg_0.95_50 | 
| Joda-Time          | detection/jodatime_cluster_vdb_50_0_allg_0.95_50 | 

## Clone pair candidates (input file)

Clone pair candidates extracted from the *Deckard* detection reports using the *deckard-extraction.ipynb* script (paths need reconfiguration).

| Project            | Location   |
|:-------------------|:-----------|
| JFreeChart         | input/jfreechart-1.0.10-deckard-selected.xls |
| Gson               | input/gson-2.8.5-deckard-selected.xls |
| Apache Commons Lang| input/commons-lang-3.7-deckard-selected.xls |
| Apache Commons IO  | input/commons-io-2.5-deckard-selected.xls |
| Joda-Time          | input/joda-time-2.10-deckard-selected.xls |

## Refactoring output

The refactored results using our technique are stored under the *refactored* folder. The correctness evaluation in the paper is based on these refactored results. The statistics in log files lead to the applicability table in the paper.

| Project            | Refactored results | log files |
|:-------------------|:-------------------|:----------|
| JFreeChart         | refactored/jfreechart-1.0.10-refactored.tar.gz |output/jfreechart-1.0.10.log |
| Gson               | refactored/gson-2.8.5-refactored.tar.gz        |output/gson-2.8.5.log |
| Apache Commons Lang| refactored/commons-lang-3.7-refactored.tar.gz  |output/commons-lang-3.7.log |
| Apache Commons IO  | refactored/commons-io-2.5-refactored.tar.gz    |output/commons-io-2.5.log |
| Joda-Time          | refactored/joda-time-2.10-refactored.tar.gz    |output/jodatime-2.10.log |

