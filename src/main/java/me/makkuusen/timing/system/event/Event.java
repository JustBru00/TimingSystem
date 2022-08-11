package me.makkuusen.timing.system.event;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.Database;
import me.makkuusen.timing.system.DatabaseTrack;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.participant.Participant;
import me.makkuusen.timing.system.participant.Spectator;
import me.makkuusen.timing.system.round.Round;
import me.makkuusen.timing.system.round.RoundType;
import me.makkuusen.timing.system.track.Track;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
public class Event {

    public static TimingSystem plugin;
    private int id;
    private UUID uuid;
    private String displayName;
    private long date;
    HashMap<UUID, Participant> participants = new HashMap<>();
    HashMap<UUID, Spectator> spectators = new HashMap<>();
    public EventSchedule eventSchedule;
    private EventState state;
    Track track;

    public enum EventState {
        SETUP, RUNNING, FINISHED
    }

    public Event(DbRow data) {
        id = data.getInt("id");
        displayName = data.getString("name");
        uuid = UUID.fromString(data.getString("uuid"));
        date = data.getLong("date");
        Optional<Track> maybeTrack = data.get("track") == null ? Optional.empty() : DatabaseTrack.getTrackById(data.getInt("track"));
        track = maybeTrack.isEmpty() ? null : maybeTrack.get();
        state = EventState.valueOf(data.getString("state"));
        eventSchedule = new EventSchedule();
    }

    public boolean start() {
        if (state != EventState.SETUP) {
            return false;
        }
        if (track == null) {
            return false;
        }
        return eventSchedule.start(this);
    }

    public boolean finish() {
        if (state == EventState.FINISHED || state == EventState.SETUP) {
            return false;
        }
        if (eventSchedule.isLastRound() && eventSchedule.getRound().isPresent() && eventSchedule.getRound().get().getState() == Round.RoundState.FINISHED) {
            setState(EventState.FINISHED);
            return true;
        }

        return false;
    }


    public void addSpectator(UUID uuid) {
        spectators.put(uuid, new Spectator(Database.getPlayer(uuid)));
    }

    public void setTrack(Track track) {
        this.track = track;
        DB.executeUpdateAsync("UPDATE `ts_events` SET `track` = " + track.getId() + " WHERE `id` = " + id + ";");
    }

    public void setState(EventState state) {
        this.state = state;
        DB.executeUpdateAsync("UPDATE `ts_events` SET `state` = '" + state.name() + "' WHERE `id` = " + id + ";");
    }

    public void quickCreate(){
        EventDatabase.roundNew(this, RoundType.QUALIFICATION, 1);
        EventDatabase.roundNew(this, RoundType.FINAL, 2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return id == event.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return displayName;
    }

    public boolean isActive() {
        return state != EventState.FINISHED;
    }
}
