package ris58h.goleador.producer;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DataAccess {
    private final String jdbcUrl;
    private final String username;
    private final String password;

    public DataAccess(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    private Connection getConnection() throws Exception {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    public Collection<Channel> loadChannels() throws Exception {
        try (
                Connection connection = getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(" SELECT channel_id, since FROM channel ");
        ) {
            Collection<Channel> result = new ArrayList<>();
            while (rs.next()) {
                String channelId = rs.getString(1);
                Long since = rs.getLong(2);
                if (rs.wasNull()) {
                    since = null;
                }
                result.add(new Channel(channelId, since));
            }
            return result;
        }
    }

    public void saveVideoIds(List<String> videoIds) throws Exception {
        try (Connection connection = getConnection()) {
//            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement(" INSERT INTO video (video_id) VALUES (?) " +
                    " ON CONFLICT DO NOTHING")) {
                for (String videoId : videoIds) {
                    ps.setString(1, videoId);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
//            connection.commit();
        }
    }

    public void updateChannelSince(String channelId, Long since) throws Exception {
        try (
                Connection connection = getConnection();
                PreparedStatement ps = connection.prepareStatement(" UPDATE channel " +
                        " SET since = ? " +
                        " WHERE channel_id = ? "
                );
        ) {
            ps.setLong(1, since);
            ps.setString(2, channelId);
            ps.executeUpdate();
        }
    }
}