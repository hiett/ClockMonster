import {JobClient} from "./clients/job-client";

export * from "./types";

(async () => {
  const client = new JobClient("http://localhost:8080");

  // Method 1
  // const job1 = await client.createJob({
  //   time: {
  //     type: "REPEATING",
  //     firstRunUnix: Date.now(),
  //     iterations: 10,
  //     interval: 60,
  //   },
  //   action: {
  //     type: "HTTP",
  //     url: "https://google.com"
  //   },
  //   payload: {
  //     userId: 234234
  //   }
  // });

  // Method 2
  const job2 = await client.builder()
    .repeating(60, 10)
    .toHttp("https://google.com") // LOL.. left a discord webhook url in there! Deleted it
    .runFirstAt(new Date())
    .withPayload({
      content: "I am a scheduled job"
    })
    .create();

  console.log(job2);
})();