// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
A meeting query is used to ideally find the times when all attendees are available.

A meeting query has two parameters, a collection of events and a meeting request.
Each event in the collection has a time range, when it takes place, and a list of attendees.
A request has a list of mandatory attendees, a list of optional attendees and a meeting duration.

If an attendee is attending an event then they are unavailable during that event's time range.
Available times returned should be greater or equal to the duration of the meeting request.
If all optional attendees and mandatory attendees are available, those times should be returned.
If there are no attendees, the whole day should be returned as available.
If the duration of the meeting is longer than a day an empty list should be returned.
If there are mandatory attendees and non of them are available an empty list should be returned.
If there are mandatory and optional attendees and the optional attendees' availabilities do not coincide
with the mandatory attendees' then only times when the mandatory attendees are free should be returned.
If there are no mandatory attendees but there are optional attendees then the times when the optional attendees 
are free should be returned.
If there are times when none of the attendees are free then an empty list should be returned. 
*/
public final class FindMeetingQuery {
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    Collection<String> attendees = request.getAttendees();
    Collection<String> optionalAttendees = request.getOptionalAttendees();
    long duration = request.getDuration();

    if (attendees.isEmpty() && optionalAttendees.isEmpty()) {
      return Arrays.asList(TimeRange.WHOLE_DAY);
    }
    if (duration > TimeRange.WHOLE_DAY.duration()) {
      return Arrays.asList();
    }

    List<TimeRange> takenTime = new ArrayList<TimeRange>();
    List<TimeRange> optionalBusy = new ArrayList<TimeRange>();
    setTakenTimes(events, takenTime, optionalBusy, attendees, optionalAttendees);

    List<TimeRange> mandatoryFree = new ArrayList<TimeRange>();
    mandatoryFree.add(TimeRange.WHOLE_DAY);
    List<TimeRange> optionalFree = new ArrayList<TimeRange>();
    setFreeTimes(mandatoryFree, takenTime);

    if(takenTime.isEmpty()) {
      optionalFree.add(TimeRange.WHOLE_DAY);
      setFreeTimes(optionalFree, optionalBusy);
      removeTooShortTimes(optionalFree, duration);
      return optionalFree;
    } 

    for(TimeRange t: mandatoryFree) {
        mutualFreeTimes(optionalFree, optionalBusy, t);
    }

    if(!optionalFree.isEmpty()) {
      removeTooShortTimes(optionalFree, duration);
      return optionalFree;
    } else {
      removeTooShortTimes(mandatoryFree, duration);
      return mandatoryFree;
    } 
  }

  private static void setTakenTimes(Collection<Event> events, List<TimeRange> takenTime, List<TimeRange> optionalBusy,
      Collection<String> attendees, Collection<String> optionalAttendees) {

    for(Event e: events) {
      Set<String> eventAtt = e.getAttendees();
      Set<String> mandatoryAtt = new HashSet<>(eventAtt);
      Set<String> optionalAtt = new HashSet<>(eventAtt);

      mandatoryAtt.retainAll(attendees);
      optionalAtt.retainAll(optionalAttendees);

      if(mandatoryAtt.isEmpty()) {
        if(!optionalAtt.isEmpty()) {
        optionalBusy.add(e.getWhen());
      }
        continue;
      }
      takenTime.add(e.getWhen());
    }
    Collections.sort(takenTime, TimeRange.ORDER_BY_START);
  }

  private static void setFreeTimes(List<TimeRange> mandatoryFree, List<TimeRange> takenTime) {
    for(TimeRange t: takenTime) {
      int lastIndex = mandatoryFree.size()-1;
      TimeRange latestRange = mandatoryFree.get(lastIndex);
      int latestStart = latestRange.start();
      int latestEnd = latestRange.end();

      int takenStart = t.start();
      int takenEnd = t.end();


//T = takenTime
//L = latestTime       
      if(latestStart >= takenStart) {
//T:    |---|
//L:         |----------| 
        if(latestStart > takenEnd) {
          continue;
        }
//T:    |--------|
//L:         |----------| 
        if(latestEnd > takenEnd) {
          setRange(mandatoryFree, lastIndex, takenEnd, latestEnd);
        } 
//T:    |-----------------|
//L:         |----------| 
        else {
          mandatoryFree.remove(lastIndex);
        }
      }
//T:                |------|
//L:         |----------| 
      else {
        setRange(mandatoryFree, lastIndex, latestStart, takenStart); 
//T:             |--|
//L:         |----------|
        if(latestEnd > takenEnd) {
          addRange(mandatoryFree, takenEnd, latestEnd);
        }
      }
    }
  }

  private static void setRange(List<TimeRange> freeTime, int index, int start, int end) {
    TimeRange newRange = TimeRange.fromStartEnd(start, end, false);
    freeTime.set(index, newRange);
  }

  private static void addRange(List<TimeRange> freeTime, int start, int end) {
    TimeRange newRange = TimeRange.fromStartEnd(start, end, false);
    freeTime.add(newRange);
    // if(newRange.duration() > duration) {
    //   freeTime.add(newRange);
    // }    
  }

  private static void mutualFreeTimes(List<TimeRange> optionalFree, List<TimeRange> optionalBusy, 
      TimeRange potentialTime) {
    boolean works = true;
    for(TimeRange t: optionalBusy) {
      if(t.overlaps(potentialTime)) {
        works = false;
        break;
      }
    }
    if(works) {
      optionalFree.add(potentialTime);
    }
  }

  private static void removeTooShortTimes(List<TimeRange> timeRanges, long duration) {
    for(int i = 0; i < timeRanges.size(); i++) {
      if(timeRanges.get(i).duration() < duration) {
        timeRanges.remove(i);
      }
    }
  }
}
