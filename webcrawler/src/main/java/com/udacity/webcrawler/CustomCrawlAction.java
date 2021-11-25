package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CustomCrawlAction extends RecursiveAction {
    private final Clock clock;
    private final List<Pattern> ignoredUrls;
    private final PageParserFactory parserFactory;
    private final String url;
    private final Instant deadline;
    private final int maxDepth;
    private final ConcurrentMap<String, Integer> counts;
    private final ConcurrentSkipListSet<String> visitedUrls;

    private CustomCrawlAction(
            Clock clock,
            List<Pattern> ignoredUrls,
            PageParserFactory parserFactory,
            String url,
            Instant deadline,
            int maxDepth,
            ConcurrentMap<String, Integer> counts,
            ConcurrentSkipListSet<String> visitedUrls
    ){
        this.clock = clock;
        this.ignoredUrls = ignoredUrls;
        this.parserFactory = parserFactory;
        this.url = url;
        this.deadline = deadline;
        this.maxDepth = maxDepth;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
    }
    @Override
    protected void compute(){
//      System.out.println(maxDepth+" "+link);
//      counter.incrementAndGet();
        if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
            return;
        }
        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return;
            }
        }
        if (!visitedUrls.add(url)) {
            return;
        }

        PageParser.Result result = parserFactory.get(url).parse();
        for (ConcurrentMap.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
            counts.compute(e.getKey(), (k, v) -> v == null ? e.getValue() : e.getValue() + v);
        }

        List<CustomCrawlAction> subActions = result.getLinks()
                .stream()
                .map(link -> new CustomCrawlAction.Builder()
                                .setClock(clock)
                                .setIgnoredUrls(ignoredUrls)
                                .setParserFactory(parserFactory)
                                .setUrl(link)
                                .setDeadline(deadline)
                                .setMaxDepth(maxDepth - 1)
                                .setVisitedUrls(visitedUrls)
                                .setCounts(counts)
                                .build()
                )
                .collect(Collectors.toList());
        invokeAll(subActions);
    }
    public static class Builder {
        private Clock clock;
        private List<Pattern> ignoredUrls;
        private PageParserFactory parserFactory;
        private String url;
        private Instant deadline;
        private int maxDepth;
        private ConcurrentMap<String, Integer> counts;
        private ConcurrentSkipListSet<String> visitedUrls;

        public Builder setClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder setIgnoredUrls(List<Pattern> ignoredUrls) {
            this.ignoredUrls = ignoredUrls;
            return this;
        }

        public Builder setParserFactory(PageParserFactory parserFactory) {
            this.parserFactory = parserFactory;
            return this;
        }
        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setDeadline(Instant deadline) {
            this.deadline = deadline;
            return this;
        }

        public Builder setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        public Builder setCounts(ConcurrentMap<String, Integer> counts) {
            this.counts = counts;
            return this;
        }

        public Builder setVisitedUrls(ConcurrentSkipListSet<String> visitedUrls) {
            this.visitedUrls = visitedUrls;
            return this;
        }

        public CustomCrawlAction build(){
            return new CustomCrawlAction(clock,
                    ignoredUrls,
                    parserFactory,
                    url,
                    deadline,
                    maxDepth,
                    counts,
                    visitedUrls);
        }
    }
}

