package ris58h.goleador.producer;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.VideoListResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class YoutubeAccess {
    private static final long MAX_RESULTS = 50L;

    private static final NetHttpTransport TRANSPORT = new NetHttpTransport();
    private static final JacksonFactory JSON_FACTORY = new JacksonFactory();

    private final YouTube youTube;
    private final String key;

    public YoutubeAccess(String key) {
        this.youTube = new YouTube.Builder(TRANSPORT, JSON_FACTORY, noop -> {
        }).build();
        this.key = key;
    }

    public List<String> getVideoIds(String channelId, long publishedAfter, long publishedBefore) throws Exception {
        List<String> result = new ArrayList<>();
        String pageToken = null;
        do {
            YouTube.Search.List search = youTube.search().list("id")
                    .setType("video")
                    .setChannelId(channelId)
                    .setFields("items(id(videoId)),nextPageToken")
                    .setOrder("date")
                    .setPublishedAfter(new DateTime(publishedAfter))
                    .setPublishedBefore(new DateTime(publishedBefore))
                    .setMaxResults(MAX_RESULTS)
                    .setKey(key);

            if (pageToken != null) {
                search.setPageToken(pageToken);
            }

            SearchListResponse searchListResponse = search.execute();
            List<SearchResult> items = searchListResponse.getItems();
            items.stream()
                    .map(searchResult -> searchResult.getId().getVideoId())
                    .forEach(result::add);
            pageToken = searchListResponse.getNextPageToken();
        } while (pageToken != null);
        return result;
    }

    public List<Video> getVideos(Collection<String> videoIds) throws Exception {
        YouTube.Videos.List list = youTube.videos().list("id,snippet,contentDetails")
                .setId(String.join(",", videoIds))
                .setFields("items(id,snippet(publishedAt),contentDetails(duration,definition))")
                .setKey(key);
        VideoListResponse videoListResponse = list.execute();
        List<com.google.api.services.youtube.model.Video> items = videoListResponse.getItems();
        return items.stream()
                .map(video -> new Video(
                        video.getId(),
                        parseDuration(video.getContentDetails().getDuration()),
                        video.getContentDetails().getDefinition(),
                        parseDateTime(video.getSnippet().getPublishedAt())
                ))
                .collect(Collectors.toList());
    }

    private static long parseDuration(String durationString) {
        Duration duration = Duration.parse(durationString);
        return duration.getSeconds();
    }

    private static long parseDateTime(DateTime dateTime) {
        return dateTime.getValue();
    }
}
