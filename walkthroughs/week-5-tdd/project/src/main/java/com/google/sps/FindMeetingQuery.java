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

public final class FindMeetingQuery {
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    Collection<String> attendees = request.getAttendees();
    Collection<String> optionalAttendees = request.getOptionalAttendees();
    long duration = request.getDuration();

    if (attendees.isEmpty() && optionalAttendees.isEmpty()){
      return Arrays.asList(TimeRange.WHOLE_DAY);
    }else 
    if (duration > TimeRange.WHOLE_DAY.duration()){
      return Arrays.asList();
    }

    List<TimeRange> takenTime = new ArrayList<TimeRange>();
    List<TimeRange> optionalBusy = new ArrayList<TimeRange>();
    setTakenTimes(events, takenTime, optionalBusy, attendees, optionalAttendees);

    List<TimeRange> mandatoryFree = new ArrayList<TimeRange>();
    mandatoryFree.add(TimeRange.WHOLE_DAY);
    List<TimeRange> optionalFree = new ArrayList<TimeRange>();
    setFreeTimes(mandatoryFree, takenTime, duration);

    if(takenTime.isEmpty()){
      optionalFree.add(TimeRange.WHOLE_DAY);
      setFreeTimes(optionalFree, optionalBusy, duration);
      return optionalFree;
    }else {
      for(TimeRange t: mandatoryFree){
        checkOptional(optionalFree, optionalBusy, t);
      }
    }

    if(!optionalFree.isEmpty()){
      return optionalFree;
    } else {
      return mandatoryFree;
    } 
  }

  private static void setTakenTimes(Collection<Event> events, List<TimeRange> takenTime, List<TimeRange> optionalBusy,
      Collection<String> attendees, Collection<String> optionalAttendees){

    for(Event e: events){
      Set<String> mandatoryAtt = new HashSet<>();
      Set<String> optionalAtt = new HashSet<>();
      Set<String> eventAtt = e.getAttendees();

      mandatoryAtt.addAll(eventAtt);
      mandatoryAtt.retainAll(attendees);
      optionalAtt.addAll(eventAtt);
      optionalAtt.retainAll(optionalAttendees);

      if(mandatoryAtt.isEmpty()){
        if(!optionalAtt.isEmpty()){
        optionalBusy.add(e.getWhen());
      }
        continue;
      }
      takenTime.add(e.getWhen());
    }
    Collections.sort(takenTime, TimeRange.ORDER_BY_START);
  }

  private static void setFreeTimes(List<TimeRange> mandatoryFree, List<TimeRange> takenTime, long duration ){
    for(TimeRange t: takenTime){
      int lastIndex = mandatoryFree.size()-1;
      TimeRange latestRange = mandatoryFree.get(lastIndex);
      int currentStart = latestRange.start();
      int currentEnd = latestRange.end();

      int takenStart = t.start();
      int takenEnd = t.end();

      if(currentStart == takenStart || currentStart > takenStart){
        if(currentStart > takenEnd){
          continue;
        }
        if(takenEnd < currentEnd){
          setRange(mandatoryFree, duration, lastIndex, takenEnd, currentEnd, false);
        }else{
          mandatoryFree.remove(lastIndex);
        }
      }else {
        setRange(mandatoryFree, duration, lastIndex, currentStart, takenStart, false); 
        if(currentEnd > takenEnd){
          addRange(mandatoryFree, duration, takenEnd, currentEnd, false);
        }
      }
    }
  }

  private static void setRange(List<TimeRange> freeTime, long duration, int index, int start, int end, boolean cond){
    TimeRange newRange = TimeRange.fromStartEnd(start, end, cond);
    if(newRange.duration() < duration){
      freeTime.remove(index);
    }else{
      freeTime.set(index, newRange);
    }
  }

  private static void addRange(List<TimeRange> freeTime, long duration, int start, int end, boolean cond){
    TimeRange newRange = TimeRange.fromStartEnd(start, end, cond);
    if(newRange.duration() > duration){
      freeTime.add(newRange);
    }    
  }

  private static void checkOptional (List<TimeRange> optionalFree, List<TimeRange> optionalBusy, 
      TimeRange potentialTime){
    boolean works = true;
    for(TimeRange t: optionalBusy){
      if(t.overlaps(potentialTime)){
        works = false;
        break;
      }
    }
    if(works){
      optionalFree.add(potentialTime);
    }
  }
}
