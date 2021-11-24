package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
  private final Clock clock;
  private final Duration timeout;
  private final int maxDepth;
  private final int popularWordCount;
  private final List<Pattern> ignoredUrls;
  private final ForkJoinPool pool;
  private final PageParserFactory parserFactory;
//  private AtomicInteger counter;

  @Inject
  ParallelWebCrawler(
      Clock clock,
      PageParserFactory parserFactory,
      @Timeout Duration timeout,
      @MaxDepth int maxDepth,
      @PopularWordCount int popularWordCount,
      @IgnoredUrls List<Pattern> ignoredUrls,
      @TargetParallelism int threadCount) {
    this.clock = clock;
    this.parserFactory = parserFactory;
    this.timeout = timeout;
    this.maxDepth = maxDepth;
    this.popularWordCount = popularWordCount;
    this.ignoredUrls = ignoredUrls;
    this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
//    this.counter = new AtomicInteger(0);
  }

  @Override
  public CrawlResult crawl(List<String> startingUrls) {
    Instant deadline = clock.instant().plus(timeout);
    ConcurrentMap<String, Integer> counts = new ConcurrentHashMap<>();
    ConcurrentSkipListSet<String> visitedUrls = new ConcurrentSkipListSet<>();
    for (String url : startingUrls) {
      pool.invoke(new CustomCrawlAction.Builder()
              .setClock(clock)
              .setIgnoredUrls(ignoredUrls)
              .setParserFactory(parserFactory)
              .setUrl(url)
              .setDeadline(deadline)
              .setMaxDepth(maxDepth)
              .setVisitedUrls(visitedUrls)
              .setCounts(counts)
              .build());
    }

    if (counts.isEmpty()) {
      return new CrawlResult.Builder()
              .setWordCounts(counts)
              .setUrlsVisited(visitedUrls.size())
              .build();
    }
//    System.out.println("Counter value: "+ counter);
    return new CrawlResult.Builder()
            .setWordCounts(WordCounts.sort(counts, popularWordCount))
            .setUrlsVisited(visitedUrls.size())
            .build();
    //return new CrawlResult.Builder().build();
  }

  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }
}
