# VIX Volatility Analysis

Traders repeat this idea constantly: earnings season is when the market gets "wild" — companies report, prices react, volatility spikes. Instead of just accepting that, I wanted to actually test it, using the VIX (Wall Street's "fear gauge") as the measure of market-wide volatility.

**H₀:** μ₁ = μ₂  **Hₐ:** μ₁ > μ₂ (one-tailed) — is the true mean daily VIX swing during earnings months (Jan/Apr/Jul/Oct) actually higher than during quiet months (Mar/Jun/Sep/Dec)? Five years of Cboe VIX data (2021–2025), α = 0.05.

## How I built the samples

I wrote a Java pipeline (`VixSampler`) to handle this end to end: load five years of trading days, group them into four strata per group by month, then randomly draw 10 days from each stratum — 40 per group, so n₁ = n₂ = 40. One thing I'm actually proud of: market holidays like Good Friday create missing rows in the VIX spreadsheet, and instead of manually patching around that, the code detects a missing row and automatically swaps in another random day from the same stratum, so I always end up with exactly 40 clean observations per group with no manual fixing.

I also deliberately excluded Feb/May/Aug/Nov — transitional months that don't cleanly belong to either group, and including them would've blurred the comparison.

## Why the 5-year window, not 1 year

This wasn't a style choice — it's required by the 10% independence condition. One year only gives ~252 trading days, so each monthly group has roughly 84 eligible days, and the 10% threshold on that is 8.4 days. I needed n=40, which blows right past that. Expanding to 5 years gives ~1,260 days, pushing the threshold to 126 — n=40 clears that comfortably.

## Result

n₁ = n₂ = 40. t ≈ −0.31, df = 39 (conservative), p ≈ 0.635 — **fail to reject H₀**. No convincing evidence that the market swings more during earnings months than quiet ones.

## Why — the Fallacy of Composition

This is the part I actually found interesting. Individual stocks absolutely spike on earnings day — Apple beats and jumps 8%, Netflix misses and drops 12%. But the VIX tracks the S&P 500 as a whole, and inside 500 stocks those individual reactions cancel out: one company beats, another misses, and the net move gets dampened. Meanwhile the "quiet" months are actually loaded with macro events — Fed rate decisions, CPI reports, end-of-quarter rebalancing — that reprice all 500 stocks at once, and those moves don't cancel the way individual earnings reactions do. That's why quiet months aren't actually quiet at the index level. What's true for one stock isn't automatically true for the whole market — classic Fallacy of Composition.

## Project structure

```
src/main/java/org/example/
  VixSampler.java     stratified sampling of trading days into earnings/quiet groups
  VixTTest.java       two-sample t-test comparing the two groups

data/
  VIX_History.csv                    raw CBOE VIX daily OHLC history
  JanuaryAprilJulyOctober.csv        trading-day pool for earnings-season months
  MarchJuneSeptemberDecember.csv     trading-day pool for quiet months
  earnings_group_sample.csv          sampled output: earnings-season days
  quiet_group_sample.csv             sampled output: quiet-month days
```

## Running it

Plain Java, no external dependencies.

```bash
cd src/main/java/org/example
javac VixSampler.java VixTTest.java

# VixSampler is currently configured for the quiet-months group (see the
# comment at the top of main) — edit targetMonths/datePoolFile/outputFile
# and re-run to regenerate the other group.
java org.example.VixSampler
java org.example.VixTTest
```

The two sample CSVs in `data/` are already generated, so `VixTTest` will run out of the box without re-sampling.
