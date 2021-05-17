# Hobbyfi: An app empowering hobbyists to communicate with each other in real time.

### Done as an ELSYS thesis project.

## 1. Purpose of the Project

**...It's in the title.**

Well, that's not all, of course. Otherwise this point wouldn't exist.

Social media apps are big, complex, multifaceted behemoths of software products functioning by virtue of micro-optimizations on even the smallest and most minutiae of implementation details. The goal of the thesis project is to emulate this manner of complexity by creating a system which has the degree of scalability and reliability that most apps of such nature are known for. A primary inspiration for **Hobbyfi** has been (and most likely always will be provided future development continues) [**Discord**](https://discord.com/).

In **Hobbyfi**, users can create chatrooms in which to discuss their various interests. They can create their own interests and put them as the headlining features of their owned chatrooms. Moreover, they can join other chatrooms even if they already own a couple: the possibilities of jumping between discussions are limitless. Chatrooms can also host events which are integrated with Google Maps. Users can track their own and other subscribed chatroom members as they make their way to events.

Everything in the app is synchronised. From simple edits to collosal deletions, the entirety of the project's difficulty was encapsulated in that real time facet of the app.

## 2. Project Architecture

Keys are not distributed in the repositories.

The  works with a REST API written in PHP (which is generally hosted on a WAMP server lest the creator has decided to pay to keep the backend up). The API requires significant setup and navigation around the WAMP architecture to be employed in order to facilitate connections from multiple sources and networks while also allowing for streamlined communication with Firebase through the Admin SDK. It is, undoubtedly, the Achilles' heel of the thesis project at the time of writing but that can always be rectified. 
 
Another component is the SocketIO server implemented in Node.js with Typescript. It, unlike the aforementioned API, is hosted on Heroku and can successfully be accessed from authorised clients.

The last big piece of the puzzle is the Firebase project created for the purpose of using Firebase Cloud Messaging and Firestore Database.

The Android client uses MVI as a baseline architecture and divulges into a somewhat more customised implementation from there on out.

## 3. Project Features
  * Full-fledged JWT authentication system (including resetting of passwords and changing of emails/passwords)
  * Facebook authentication & SDK integration
  * CRUD for basic resources like chatrooms, users, interests, messages, and events. Image uploading with CameraX and SAF-compliant media access integration.
  * Real time chat system (with image sending) and synchronisation events built-in for possible user edge-cases
  * Google Maps integrated events with pinpoint accurate location updates to Cloud Firestore using a Foreground service
  * Ability to share events in Facebook as fully responsive and contextually aware deep links
  * ...And many more small notes to give the app a polished feel: UI driven by Material Design principles and guidelines, connection awareness, multi-layered fetching system (cache with network), pagination for big content; so the list goes on!

## 4. Project Gallery

Some screenshots of the project, courtesy of yours truly.

| | | |
|:-------------------------:|:-------------------------:|:-------------------------:|
|<img width="1604" alt="screen shot 2017-08-07 at 12 18 15 pm" src="https://cdn.discordapp.com/attachments/589145476654301235/840594200424742962/Screenshot_2021-05-08-17-10-48-332_com.example.hobbyfi.jpg">  Normal chat |  <img width="1604" alt="screen shot 2017-08-07 at 12 18 15 pm" src="https://cdn.discordapp.com/attachments/589145476654301235/840594200734990386/Screenshot_2021-05-08-17-11-04-399_com.example.hobbyfi.jpg">  Events |<img width="1604" alt="screen shot 2017-08-07 at 12 18 15 pm" src="https://cdn.discordapp.com/attachments/589145476654301235/840594254543454208/Screenshot_2021-05-08-17-11-36-059_com.example.hobbyfi.jpg">Chatroom interests |
|<img width="1604" alt="screen shot 2017-08-07 at 12 18 15 pm" src="https://cdn.discordapp.com/attachments/589145476654301235/840594254815952946/Screenshot_2021-05-08-17-11-42-005_com.example.hobbyfi.jpg"> Entry screen à la simple |  <img width="1604" alt="screen shot 2017-08-07 at 12 18 15 pm" src="https://cdn.discordapp.com/attachments/589145476654301235/840594293110472704/Screenshot_2021-05-08-17-12-20-945_com.example.hobbyfi.jpg"> Camera capture inception |<img width="1604" alt="screen shot 2017-08-07 at 12 18 15 pm" src="https://cdn.discordapp.com/attachments/589145476654301235/840594293604745276/Screenshot_2021-05-08-17-12-52-912_com.example.hobbyfi.jpg"> Google Maps event with another user (red) for a specified location (cyan) |
|<img width="1604" alt="screen shot 2017-08-07 at 12 18 15 pm" src="https://cdn.discordapp.com/attachments/589145476654301235/840594481245454386/IMG_20210508_172215.jpg"> Location fetching notification (i.e. Foreground service) |  <img width="1604" alt="screen shot 2017-08-07 at 12 18 15 pm" src="https://cdn.discordapp.com/attachments/589145476654301235/840606143504187412/IMG_20210508_172241.jpg"> Push notification upon new message |<img width="1604" alt="screen shot 2017-08-07 at 12 18 15 pm" src="https://cdn.discordapp.com/attachments/589145476654301235/840606763932975104/IMG_20210508_181105.jpg"> Event shared as a post on Facebook. Can be used as a deeplink. |

## 5. MAD Score

Following MVI and using a variety of Jetpack libraries alongside is reflected in the MAD scorecard generated for the project:

![](https://cdn.discordapp.com/attachments/503981813358788629/836661904901406730/unknown.png)

![](https://cdn.discordapp.com/attachments/503981813358788629/836662306939207740/unknown.png)

## 6. Repositories of interest

### [Hobbyfi-API](https://github.com/GGLozanov/Hobbyfi-API)

### [Hobbyfi-SocketIO](https://github.com/GGLozanov/Hobbyfi-SocketIO)

## 7. [Thesis](https://docs.google.com/document/d/1iA1oZQ3cWXrrpBV3YvDrT3UakdvVSLQUMXad_BNIdU4/edit?usp=sharing)
