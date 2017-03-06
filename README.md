# Android-Dropbox-UploadImage-To-SpecificFolder-By-FolderSelection
An Android project in which you can select a particular folder and upload your images(can be any file as per your needs). 

1)Current dropbox folder selection is limited to parent folders. 
For inner folders you can modify traverse logic in MainActivity->FolderScanTask class by filling ArrayList with sub folder strings.

2)Get your API Keys from dropbox developer site and replace in AndroidManifest.xml and Strings.xml and you are good to go

References are taken from: https://www.sitepoint.com/adding-the-dropbox-api-to-an-android-app/
