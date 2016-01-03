package com.eigenmusik.sources.soundcloud;

import com.eigenmusik.exceptions.SourceAuthenticationException;
import com.eigenmusik.sources.Source;
import com.eigenmusik.sources.SourceAccount;
import com.eigenmusik.sources.SourceAccountRepository;
import com.eigenmusik.sources.SourceService;
import com.eigenmusik.tracks.Track;
import com.eigenmusik.tracks.TrackSource;
import com.eigenmusik.tracks.TrackStreamUrl;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@ConfigurationProperties(prefix = "soundcloud")
public class SoundcloudService extends SourceService {

    private static Logger log = Logger.getLogger(SoundcloudService.class);
    private SoundcloudGateway soundcloudGateway;
    private SoundcloudAccessTokenRepository soundcloudAccessTokenRepository;
    private SoundcloudUserRepository soundcloudUserRepository;

    @Autowired
    public SoundcloudService(
            SourceAccountRepository sourceAccountRepository,
            SoundcloudGateway soundcloudGateway,
            SoundcloudAccessTokenRepository soundcloudAccessTokenRepository,
            SoundcloudUserRepository soundcloudUserRepository) {
        super(sourceAccountRepository);
        this.soundcloudGateway = soundcloudGateway;
        this.soundcloudAccessTokenRepository = soundcloudAccessTokenRepository;
        this.soundcloudUserRepository = soundcloudUserRepository;
    }

    public SourceAccount getAccount(String authCode) throws SourceAuthenticationException {

        try {
            SoundcloudAccessToken soundcloudAccessToken = soundcloudGateway.exchangeToken(authCode);
            SoundcloudUser soundcloudUser = soundcloudGateway.getMe(soundcloudAccessToken);
            soundcloudUser.setAccessToken(soundcloudAccessToken);

            soundcloudAccessTokenRepository.save(soundcloudAccessToken);
            soundcloudUserRepository.save(soundcloudUser);

            SourceAccount account = new SourceAccount();
            account.setUri(soundcloudUser.getId());
            account.setSource(Source.SOUNDCLOUD);
            return account;
        } catch (IOException e) {
            SourceAuthenticationException sourceAuthenticationException = new SourceAuthenticationException();
            sourceAuthenticationException.setSource(Source.SOUNDCLOUD);
            throw sourceAuthenticationException;
        }
    }

    public List<Track> getTracks(SourceAccount account) {
        List<Track> tracks = new ArrayList<>();
        SoundcloudUser soundcloudUser = soundcloudUserRepository.findOne(account.getUri());
        try {
            List<SoundcloudTrack> soundcloudTracks = soundcloudGateway.getTracks(soundcloudUser);

            tracks = soundcloudTracks.stream().map(t -> mapToTrack(t, account)).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tracks;
    }

    public TrackStreamUrl getStreamUrl(Track track) {
        log.info(track.getTrackSource().getOwner().getUri());
        SoundcloudUser soundcloudUser = soundcloudUserRepository.findOne(Long.valueOf(track.getTrackSource().getOwner().getUri()));
        return new TrackStreamUrl(soundcloudGateway.getStreamUrl(Long.valueOf(track.getTrackSource().getUri()), soundcloudUser.getAccessToken()));
    }

    // TODO where should this live?
    private Track mapToTrack(SoundcloudTrack t, SourceAccount account) {
        TrackSource trackSource = new TrackSource();
        trackSource.setUri(t.getSoundcloudId().toString());
        trackSource.setSource(Source.SOUNDCLOUD);
        trackSource.setOwner(account);

        Track track = new Track();
        track.setName(t.getTitle());
        track.setArtist(t.getArtist());
        track.setTrackSource(trackSource);
        return track;
    }

}