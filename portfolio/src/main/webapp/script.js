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

/* exported loadCommentSection */
/* global google */
// Neccessary constants or else variables will return as
// 'undefined' in lint checks

/** Fetches comments from the server and adds them to the DOM. */
function loadCommentSection() {
  fetchBlobUrlAndShowForm();

  fetch('/data').then((response) => response.json()).then((data) => {
    const commentElements = document.getElementById('comments');
    commentElements.innerText = '';
    const limit = document.getElementById('commentLimit').value;
    const limitMap = new Map();
    let count = 0;

    data.forEach((comment) => {
      if (count < limit) {
        commentElements.appendChild(createCommentElement(comment));
        count++;
      }
      const mapKey = comment.commentLimit;
      const currentLimit = limitMap.has(mapKey) ? limitMap.get(mapKey) : 0;
      limitMap.set(mapKey, currentLimit + 1);
    });

    google.charts.load('current', {packages: ['corechart']});
    google.charts.setOnLoadCallback(function() {
      drawChart(limitMap);
    });
  });
}

/** Creates an element that represents a comment with a delete button. */
function createCommentElement(comment) {
  const commentElement = document.createElement('li');
  commentElement.className = 'comment';

  const textElement = document.createElement('span');
  textElement.className = 'text';
  textElement.innerText = comment.message;

  const nameElement = document.createElement('span');
  nameElement.className = 'name';
  nameElement.innerText = comment.author + ':';

  commentElement.appendChild(nameElement);
  commentElement.appendChild(textElement);

  const imageURL = comment.imageURL;
  if (imageURL != null) {
    const imgElement = document.createElement('img');
    imgElement.className = 'image';
    imgElement.setAttribute('src', imageURL);

    commentElement.appendChild(imgElement);
  }

  return commentElement;
}

/** Creates a chart and adds it to the page. */
function drawChart(limitMap) {
  const data = new google.visualization.DataTable();
  data.addColumn('string', 'Limit');
  data.addColumn('number', 'Count');

  for (const [key, value] of limitMap.entries()) {
    data.addRow([key, value]);
  }

  const options = {
    'title': 'Comment Limits',
    'width': 500,
    'height': 400,
  };

  const chart =
      new google.visualization.PieChart(document.getElementById('chart-div'));
  chart.draw(data, options);
}

function fetchBlobUrlAndShowForm() {
  fetch('/blobstore-url')
      .then((response) => {
        return response.text();
      })
      .then((imageUploadUrl) => {
        const messageForm = document.getElementById('form');
        messageForm.action = imageUploadUrl;
        messageForm.classList.remove('hidden');
      });
}
