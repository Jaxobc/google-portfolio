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

/** Fetches comments from the server and adds them to the DOM. */
function loadComments() {
  fetch('/data').then(response => response.json()).then(data => {
    const commentElements = document.getElementById('comments');

    data.forEach(comment => {
      commentElements.appendChild(createCommentElement(comment));
    })
  });
}

/**Creates an element that represents a comment with a delete button. */
function createCommentElement(comment) {
  const commentElement = document.createElement('li');
  commentElement.className = 'comment';

  const textElement = document.createElement('span');
  textElement.className = 'text';
  textElement.innerText = comment.message;

  const nameElement = document.createElement('span');
  nameElement.className = 'name';
  nameElement.innerText = comment.author + ":";

  commentElement.appendChild(nameElement);
  commentElement.appendChild(textElement);
  return commentElement;
}
