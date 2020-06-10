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

package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.sps.data.Comment;
import com.google.gson.Gson;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.ArrayList;


/** Servlet that returns some example content. TODO: modify this file to handle comments data */
@WebServlet("/data")
public class DataServlet extends HttpServlet {
  private final static String tblTitle = "Comment";
  private final static String tblComment = "comment";
  private final static String tblName = "name";
  private final static String tblLimit = "limit";
  private final static String tblTime = "timestamp";

  private final static String htmlComment = "text-input";
  private final static String htmlName = "name";
  private final static String htmlLimit = "commentLimit";

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Query query = new Query(tblTitle).addSort(tblTime, SortDirection.DESCENDING);;

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);
    
    
   
    List<Comment> comments = new ArrayList<Comment>();
    int count = 0;
    int limit = 0;
    for (Entity entity : results.asIterable()) {
      if (count == 0) {
        limit = Integer.parseInt((String) entity.getProperty(tblLimit));
      }

      if(count < limit){
        String commentString = (String) entity.getProperty(tblComment);
        String name = (String) entity.getProperty(tblName);
        long id = entity.getKey().getId();

        Comment comment = new Comment(id, commentString, name);
        comments.add(comment);
        count++;
      }else {
          break;
      }
    }

    Gson gson = new Gson();

    // Send the JSON as the response
    response.setContentType("application/json;");
    response.getWriter().println(gson.toJson(comments));
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Get the input from the form.
    String text = request.getParameter(htmlComment);
    String name = request.getParameter(htmlName);
    String limit = request.getParameter(htmlLimit);
    long timestamp = System.currentTimeMillis();

    // Add input to current comments in datastore.
    Entity taskEntity = new Entity(tblTitle);
    taskEntity.setProperty(tblComment, text);
    taskEntity.setProperty(tblName, name);
    taskEntity.setProperty(tblTime, timestamp);
    taskEntity.setProperty(tblLimit, limit);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(taskEntity);

    // Redirect to index page.
    response.sendRedirect("/index.html");
  }  
}