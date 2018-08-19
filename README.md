# *JRefactoring*: parameterization refactoring tool

## Summary

|       Benchmark   | Total tests | Detected clone pairs | Parameterized clone pairs |Effective candidates | Refactorable | Refactoring ratio |
|:-----------------:|:-----------:|:--------------------:|:-------------------------:|:-------------------:|:------------:|:-----------------:|
| jfreechart-1.0.10 |     3934    |         272          |           71              |          62         |      51      |        82.3%      |
| gson-2.8.5        |     1050    |          31          |           14              |          14         |      10      |        71.4%      |
| commons-lang-3.7  |     4068    |         144          |          103              |          93         |      65      |        69.9%      |
| commons-io-2.5    |     1157    |          78          |           57              |          56         |      38      |        67.9%      |
| jodatime-2.10     |     4222    |         249          |                           |                     |              |                   |

- Detected clone pairs: clone pairs directly detected from clone detection tools;
- Parameterized clone pairs: parameterizable clone candidates identified by *JRefactoring*, possibly including candidates which are not test cases, and repeated naming;
- Effective candidates: test clone pairs without repetition;
- Refactorable: test clone pairs that are successfully refactored by *JRefactoring* (passed with same coverage);
- Refactoring ratio: refactorable / effective candidates.


## Common launch config

Workspace Data Location:
workspace

VM arguments: 
-Xms128m -Xmx8192m

Working directory:
/home/yannick/workspace/eclipse

## JFreeChart 1.0.10

skipped cases:
- TLE, *TestEquals* (JDeodorant): 2356,2889,5249,5315,8572

<!---
- repeated refactoring (JDeodorant): 7821
- no method parameter mapping (JDeodorant): 8816
- private field access in different files: 1674,3715
- not test code: 8097
--->

Program arguments:
-p jfreechart-1.0.10
-x jrefactoring/results/jfreechart-1.0.10/jfreechart-1.0.10-selected.xls
-m ANALYZE_EXISTING
-l
-s 2356,2889,5249,5315,8572
-pkg org.jfree
-ac

## Gson 2.8.5

Program arguments:
-p gson
-x jrefactoring/results/gson-2.8.5/gson-2.8.5-deckard-selected.xls
-m ANALYZE_EXISTING 
-l
-pkg com.google.gson
-ac

## Apache Commons IO 2.5

Program arguments:
-p commons-io-2.5
-x jrefactoring/results/commons-io-2.5/commons-io-2.5-deckard-selected.xls
-m ANALYZE_EXISTING 
-l
-pkg org.apache.commons.io
-ac

## Apache Commons Lang 3.7

Program arguments:
-p commons-lang-3.7
-x jrefactoring/results/commons-lang-3.7/commons-lang-3.7-deckard-selected.xls
-m ANALYZE_EXISTING 
-l
-pkg org.apache.commons.lang3
-s 16,105
-ac

## Joda-Time 2.10

Program arguments:
-p joda-time-2.10
-x jrefactoring/results/jodatime-2.10/joda-time-2.10-deckard-selected.xls
-m ANALYZE_EXISTING 
-l
-pkg org.joda.time
-s 3
-ac

## Alternative for data parameterization refactoring

*JUnit Theories* could be better choice when test clone pairs only have data-parameterizable difference:

- [Javadoc](https://junit.org/junit4/javadoc/4.12/org/junit/experimental/theories/Theories.html)
- [Github Wiki](https://github.com/junit-team/junit4/wiki/theories)
