# Instructions
## Prerequisites
FTP server with sub-directories for each institution.

Access to the Developer Network for all member institutions, including the remote-storage institutions.

### For All Institutions:
1. FTP connection configuration - To share files between the App and Alma with a  [sub-directory](https://knowledge.exlibrisgroup.com/Alma/Product_Documentation/010Alma_Online_Help_(English)/050Administration/050Configuring_General_Alma_Functions/050External_Systems#UpdateSubmissionFormatFtp) for each institution (the directory name should include the institution code. e.g. main_folder/01AAA_ABC).
2. API-key with r/w permission for Bibs + Conf + Users
3. Remote Storage Facility - To export all requests to remote storage
    - Create an [integration profile](https://developers.exlibrisgroup.com/alma/integrations/remote_storage/xml_based/)  of “remote storage” type. “Export File Path“ is "requests".
    - Create a [Remote Storage](https://knowledge.exlibrisgroup.com/Alma/Product_Documentation/010Alma_Online_Help_(English)/030Fulfillment/080Configuring_Fulfillment/040Configuring_Remote_Storage_Facilities) connected to your integration profile.
    - Edit [Physical Location](https://knowledge.exlibrisgroup.com/Alma/Product_Documentation/010Alma_Online_Help_(English)/030Fulfillment/080Configuring_Fulfillment/030Configuring_Physical_Locations) - Type is : Remote Storage , Remote Storage is the remote storage facility you created.
    - Find the job ID that should be used for submitting the job and add it to the app configuration. Find the Integration Profile ID using /almaws/v1/conf/integration-profiles?type=REMOTE_STORAGE, and use it to call: /almaws/v1/conf/jobs?profile_id={ID}. Grab the job ID whose name starts with “Inventory Remote Storage Update” and paste that in conf.json under requests_job_id.
4. Publishing Profile - To handle items synchronization
    - Create a [set](https://knowledge.exlibrisgroup.com/Alma/Product_Documentation/010Alma_Online_Help_(English)/050Administration/070Managing_Jobs/060Managing_Search_Queries_and_Sets#sets.setDetail) which contains all items that are located in the remote storage location.
    - Create an Items [publishing profile job](https://knowledge.exlibrisgroup.com/Alma/Product_Documentation/010Alma_Online_Help_(English)/090Integrations_with_External_Systems/030Resource_Management/080Publishing_and_Inventory_Enrichment) with the above set. The publishing protocol should be FTP and the sub-directory is "items". Use the compressed file extension: tar.gz
    - The Job should contain the physical items enrichment with the following: Repeatable field is ITM ,Barcode subfield:b,Current library subfield:c,Current location subfield:l
    - Take the Profile Id from the UI and use it with this API: /almaws/v1/conf/jobs?profile_id={ID}. Write that ID in conf.json under publishing_job_id.
    - Important note: Any change to the publishing/integration profile might change the related job ID.
    - When running the Item Publishing for the first time, all existing items will be published, and therefore we need to make sure that the App is turned off, so it will not process all the items.
5.  [Webhooks](https://knowledge.exlibrisgroup.com/Alma/Product_Documentation/010Alma_Online_Help_(English)/090Integrations_with_External_Systems/030Resource_Management/300Webhooks)
    - Create a Webhooks Integration Profile. Message type should be JSON and under Subscriptions Select `Job Finish` to send a webhook when an Alma Job is done. Webhook listener URL will be the url after deploying the app following forward slash and "webhook". E.g.:  https://alma-remote-storage-app.herokuapp.com/webhook
6. For institution which are still not part of the processed, but could be a pick location for institutions which are, they need to be configured in conf.json but only with an API-key.

### For the Remote Storage Institution:
1. Create a patron for each Institution/Library (which can be a pickup location). For example if the Institution code is 01AAA_ABC and the library code is RS, the user's primary identifier will be 01AAA_ABC-RS. (The user should have a home address.)
2. Define [provenance code](https://knowledge.exlibrisgroup.com/Alma/Product_Documentation/010Alma_Online_Help_(English)/040Resource_Management/080Configuring_Resource_Management/080Configuring_Provenance_Codes) for each institution code.
3. Add personal-delivery for all items' [terms of use](https://knowledge.exlibrisgroup.com/Alma/Product_Documentation/010Alma_Online_Help_(English)/030Fulfillment/080Configuring_Fulfillment/050Physical_Fulfillment#fulfillment.tou.termsOfUseManagement)
4. Create a [Webhooks](https://knowledge.exlibrisgroup.com/Alma/Product_Documentation/010Alma_Online_Help_(English)/090Integrations_with_External_Systems/030Resource_Management/300Webhooks) Integration Profile. Where the Message type is JSON and Under Subscriptions select `Loans`. Webhook listener URL will be (same as above) the url after deploying the app following forward slash and "webhook". E.g.:  https://alma-remote-storage-app.herokuapp.com/webhook


## Installation

1. Install [git](https://git-scm.com/downloads).
2. Install [Heroku](https://devcenter.heroku.com/articles/getting-started-with-java#set-up).
3. Clone this repository: `git clone https://github.com/ExLibrisGroup/AlmaRemoteStorageApp.git`
4. Go to the repository folder `cd AlmaRemoteStorageApp`
5. Remove the .git folder.
6. The file conf.json should include confidential information, so we'll not upload it to Heroku. Move `conf.json` out to the FTP server, under main-folder and update the relevant values: Gateway url, API-keys etc.
7. Commit to Git: `git init` , `git add .` , `git commit -m "Ready to deploy"`
8. Create the heroku app `heroku create “app-name“`
9. Add conf.json path to the [Config Vars](https://devcenter.heroku.com/articles/config-vars#using-the-heroku-dashboard) when Key=CONFIG_FILE and Value=ftp://user:password@server/path/to/conf.json
if you are using your own server then you can copy the conf.json file into the src/main/resources folder
10. Deploy your code `git push heroku master`. The application is now deployed. Ensure that at least one instance of the app is running: `heroku ps:scale web=1`
11. Congratulations! Your web app should now be up and running on Heroku. If you like to test it from your browser, open it with: `heroku open`
12. The URL that now opened in your browser is the URL you need to configre in the Webhook integration profile.
13. When configuring the Webhook profile, press on "Activate". This will call the "challenge" URL: https://alma-remote-storage-app.herokuapp.com/webhook?challenge=123

## Maintaining Historic Log Files
Heroku doesn't keep log files above 1500 lines. For troublshooting we added support for uploading log files to the FTP.
It is done by a job scheduled in Heroku. However since we use a free account in Heroku, scheduled jobs are not guaranteed to run.
Free dynos "sleep" after 30 minutes of inactivity. They can be waken up by calling our App URL every few minutes.
The below script can be run from any Windows PC to prevent your Heroku App from sleeping:
See other tricks [here](https://quickleft.com/blog/6-easy-ways-to-prevent-your-heroku-node-app-from-sleeping/)

- RepeatCurl.bat file :
```
@echo OFF
:REPEAT
@echo. %date% at %time% >>CurlLogs.txt
curl  “https://alma-remote-storage-app.herokuapp.com“
timeout /t 1800 /nobreak > NUL
goto REPEAT
```

