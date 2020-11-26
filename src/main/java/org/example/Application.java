package org.example;

import org.example.exceptions.InvalidImageExtensionException;
import org.example.exceptions.NoObjectsException;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class Application {
    private static final Region AWS_REGION = Region.US_WEST_2;
    private static final String BUCKET_NAME = "arkharov-dias-cloudphoto";
    private static final String TAG_KEY = "cloudphoto";
    private static final String DELIMITER_WHITESPACE = "ᴥ";
    private static final char DELIMITER_PATH = '/';
    private static final String WHITESPACE = " ";
    private static final String IMAGE_EXTENSION_JPG = "jpg";
    private static final String IMAGE_EXTENSION_JPEG = "jpeg";
    private static final String IMAGE_EXTENSION_PNG = "png";
    private static final String MESSAGE_SUCCESS = "+ SUCCESS ------> ";
    private static final String MESSAGE_FAILED = "- FAILED  ------> ";
    private static final String MESSAGE_REASON = "? REASON  ======> ";
    private static final String REGEX_USER_INPUT_DATA = "^\\s*cloudphoto\\s+((((upload|download)\\s+-p\\s+(\\\\*\\/*\\b((\\S+\\s*)|(\\s*\\S+))\\b\\/*\\\\*)*\\s+-a\\s+\\b\\S+\\b\\s*$)|(list\\s*($|-a\\s+\\S+\\s*$)))|exit\\s*$)";
    private static final String REGEX_IMAGE_NAME = "([^\\s]+(\\s{0,10}\\S\\s{0,10})+(\\.(?i)(" + IMAGE_EXTENSION_JPG + "|" + IMAGE_EXTENSION_PNG + "|" + IMAGE_EXTENSION_JPEG + "))$)";
    private static final int PATH_INDEX = 1; // index of string array that contains -p value(path) for replacing whitespaces
    private static S3Client s3Client;

    public static void main(String[] args){

        Application.init();

        Scanner scanner = new Scanner(System.in);

        boolean flag = true;

        while (flag){
            String inputData = scanner.nextLine();

            if (isValidCommandInputData(inputData)){
                Command command = getCommandType(inputData);
                switch (command.getCommandType()) {
                    case UPLOAD:
                        executeUpload(command);
                        break;
                    case DOWNLOAD:
                        executeDownload(command);
                        break;
                    case LIST:
                        executeListing(command);
                        break;
                    case EXIT:
                        flag = false;
                        break;
                    default:
                        System.out.println("!No such command:");
                }
            }else {
                System.out.println("!Invalid command!");
            }

        }

    }

    private static void init(){
        initS3Client();
        printIntroductionInfo();
        prepareBucket();
    }

    private static void printIntroductionInfo(){
        System.out.println(
                        "-------------------------------------------------------------------------------------------\n" +
                        "CLOUDPHOTO(1)                         November 2020                         CLOUDPHOTO(1)\n" +
                        "\n" +
                        "NAME:\n" +
                        "       cloudphoto - CLI for Amazon s3\n" +
                        "SYNOPSIS:\n" +
                        "       cloudphoto  COMMAND [OPTION]... [ARG]...\n" +
                        "DESCRIPTION:\n" +
                        "       cloudphoto is a client for interacting with the amazon object storages s3.\n" +
                        "       The cloudphoto CLI has 4 commands. The commands are listed below.\n" +
                        "OPTIONS:\n" +
                        "       -p      path of directory\n" +
                        "       -a      album name\n" +
                        "COMMANDS:\n" +
                        "       upload      upload files to object storage s3 from directory by a specific album\n" +
                        "       download    download files from object storage s3 with album name to directory\n" +
                        "       list        list objects from object storage s3\n" +
                        "       exit        quit from program\n" +
                        "EXAMPLES:\n" +
                        "       upload      cloudphoto upload -p /home/username/photos/ -a holiday\n" +
                        "       download    cloudphoto download -p /home/username/downloads/ -a holiday\n" +
                        "       list        cloudphoto list -a holiday\n" +
                        "                   cloudphoto list\n" +
                        "       exit        cloudphoto exit\n" +
                        "-------------------------------------------------------------------------------------------");
    }

    private static void initS3Client(){
        s3Client = S3Client.builder()
                .region(AWS_REGION)
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();
    }

    private static void executeUpload(Command command){
        try {
            uploadImages(command.getParameterValue(Argument.PATH), command.getParameterValue(Argument.ALBUM));
            //System.out.println("SUCCESS");
        }catch (FileNotFoundException fileNotFoundException){
            System.out.println(fileNotFoundException.getMessage());
        } /*catch (InvalidImageExtensionException invalidImageExtensionException) {
            System.out.println("The picture" + "\"" + invalidImageExtensionException.getMessage() + "\"" + "from the directory has an invalid extension. Use jpg, jpeg or png.");
        }*/
    }

    private static void executeDownload(Command command){
        try {
            downloadImages(command.getParameterValue(Argument.PATH), command.getParameterValue(Argument.ALBUM));
            //System.out.println("SUCCESS");
        } catch (FileNotFoundException | NoObjectsException exception) {
            System.out.println(exception.getMessage());
        } /*catch (InvalidImageExtensionException invalidImageExtensionException) {
            System.out.println("The picture" + "\"" + invalidImageExtensionException.getMessage() + "\"" + " from the bucket has an invalid extension. Use jpg, jpeg or png.");
        }*/
    }

    private static void executeListing(Command command){
        if (command.getParameters().isEmpty()){
            executeAlbumsListing();
        }else {
            executeAlbumImagesListing(command);
        }

    }

    private static void executeAlbumsListing(){
        List<Tag> tagList = getListBucketObjectsByTag("", Tag.builder().build());

        if (!tagList.isEmpty()){
            tagList.stream()//if no tag list is empty
                    .distinct()
                    .forEach(n -> System.out.println("*" + n.value()));
        }else {
            System.out.println("!No albums");
        }
    }

    private static void executeAlbumImagesListing(Command command){
        String tagValue = command.getParameterValue(Argument.ALBUM);

        List<S3Object> tagList = getListBucketObjectsByTag(tagValue, S3Object.builder().build());

        if (!tagList.isEmpty()){
            tagList.forEach(n -> System.out.println("*" + n.key()));
        }else {
            System.out.println("!No images of " + "\"" + tagValue + "\"" + " album");
        }

    }

    private static Command getCommandType(String inputData){
        boolean flag = inputData.contains(Argument.PATH.getArgumentName());

        String[] filteredData = flag
                ? whiteSpaceFilter(replaceWhitespacesInPath(inputData))
                : whiteSpaceFilter(inputData);

        CommandType commandType = CommandType.valueOf(filteredData[1].toUpperCase());
        Command command = new Command(commandType);

        for (int i = 1; i < filteredData.length-1; i = i + 2) {
            filteredData[i+2] = flag
                    ? filteredData[i+2].replace(DELIMITER_WHITESPACE, WHITESPACE)
                    : filteredData[i+2];
            command.setParameter( filteredData[i+1], filteredData[i+2]);//[i+1]=key [i+2]=value for argument.For example -a albumName
        }
        return command;
    }

    /*private static boolean containsWhitespaces(String inputData){
        return findPath(inputData).contains(" ");
    }*/

    private static String replaceWhitespacesInPath(String inputData){
        String pathWithWhitespaces = findPath(inputData).trim();
        String modifiedPath = pathWithWhitespaces.replace(WHITESPACE, DELIMITER_WHITESPACE);
        return inputData.replace(pathWithWhitespaces, modifiedPath);
    }

    private static String findPath(String inputData){
        return inputData
                .split( Argument.PATH.getArgumentName() + "|" + Argument.ALBUM.getArgumentName())[PATH_INDEX];
    }


    private static String[] whiteSpaceFilter(String inputData){
        return Arrays.stream(inputData.split(WHITESPACE))
                .filter(n -> !n.isEmpty())
                .toArray(String[]::new);
    }


    private static boolean isValidCommandInputData(String inputData){
        return inputData.matches(REGEX_USER_INPUT_DATA);
        //return inputData.matches("^\\s*cloudphoto\\s+(((upload|download)\\s+-p\\s+(\\\\?\\b\\S+\\b\\\\?)*\\s+-a\\s+\\b\\S+\\b\\s*$)|(list\\s*($|-a\\s+\\S+\\s*$)))");

    }

    private static void downloadImages(String pathName, String tagValue) throws NoSuchBucketException, FileNotFoundException, NoObjectsException {
        List<S3Object> s3ObjectList = getListBucketObjectsByTag(tagValue, S3Object.builder().build());

        if (s3ObjectList.isEmpty()){
            throw new NoObjectsException("!No such album");
        }else if (!new File(pathName).exists()){
            throw new FileNotFoundException("!No such folder: " + pathName);
        }else {
            int successCount = 0;
            for (S3Object s3Object: s3ObjectList) {
                try {
                    processImageName(s3Object.key());
                    downloadImage(s3Object.key(), processPath(pathName));
                    successCount++;
                    System.out.println(MESSAGE_SUCCESS + s3Object.key());
                }catch (InvalidImageExtensionException invalidImageExtensionException){
                    System.out.println(MESSAGE_FAILED + s3Object.key());
                    System.out.println(MESSAGE_REASON + invalidImageExtensionException.getMessage());
                }

            }
            int totalSize = s3ObjectList.size();
            System.out.println("TOTAL: " + totalSize);
            System.out.println("SUCCESS: " + successCount + "/" + totalSize);
            System.out.println("FAILED: " + (totalSize - successCount) + "/" + totalSize);
        }


    }

    private static String processPath(String pathName){
        return pathName.charAt(pathName.length() - 1) != DELIMITER_PATH ? pathName.concat(String.valueOf(DELIMITER_PATH)) : pathName;
    }

    private static void downloadImage(String keyName, String pathName) {

        GetObjectRequest objectRequest = GetObjectRequest
                .builder()
                .key(keyName)
                .bucket(BUCKET_NAME)
                .build();

        // get the byte[] from this AWS S3 object
        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(objectRequest);
        byte[] data = objectBytes.asByteArray();

        try (FileOutputStream fos = new FileOutputStream(pathName + keyName)) {
            fos.write(data);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }


    }

    public static <T> List<T> getListBucketObjectsByTag(String tagValue, T element) {

        List<T> resultList = new ArrayList<>();

        try {
            ListObjectsRequest listObjects = ListObjectsRequest
                    .builder()
                    .bucket(BUCKET_NAME)
                    .build();

            ListObjectsResponse listObjectsResponse = s3Client.listObjects(listObjects);
            List<S3Object> s3ObjectList = listObjectsResponse.contents();

            for (S3Object s3Object : s3ObjectList) {

                GetObjectTaggingResponse result = s3Client.getObjectTagging(GetObjectTaggingRequest.builder()
                        .bucket(BUCKET_NAME)
                        .key(s3Object.key())
                        .build()
                );

                List<Tag> tagList = result.tagSet();

                if (!tagList.isEmpty()){
                    for (Tag tag :tagList){
                        if (tagValue.isEmpty()){
                            if (containsTagKey(tag)){
                                resultList.add((T) tag);
                            }
                        }else {
                            if (isValidTag(tag, tagValue)){
                                resultList.add((T) s3Object);
                            }

                        }
                    }
                }
            }

        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }

        return resultList;
    }

    private static boolean isValidTag(Tag tag, String tagValue) {
        return  tagValue.isEmpty() ? containsTagKey(tag) : containsTagKey(tag) && tag.value().equalsIgnoreCase(tagValue);
    }

    private static boolean containsTagKey(Tag tag){
        return  !tag.key().isEmpty() &&
                !tag.value().isEmpty() &&
                tag.key().equalsIgnoreCase(TAG_KEY);
    }


    private static void uploadImages(String folderPath, String tagValue) throws FileNotFoundException {
        File folder = new File(folderPath);

        if (folder.exists()){
            File [] listOfFiles = folder.listFiles();
            if (listOfFiles != null){
                int successCount = 0;
                for (File file : listOfFiles) {
                    if (file.isFile()) {
                        try {
                            processImageName(file.getName());
                            uploadImage(file, tagValue);
                            successCount++;
                            System.out.println(MESSAGE_SUCCESS + file.getName());
                        }catch (InvalidImageExtensionException invalidImageExtensionException){
                            System.out.println(MESSAGE_FAILED + file.getName());
                            System.out.println(MESSAGE_REASON + invalidImageExtensionException.getMessage());
                        }
                    }
                }
                int totalSize = listOfFiles.length;
                System.out.println("TOTAL: " + totalSize);
                System.out.println("SUCCESS: " + successCount + "/" + totalSize);
                System.out.println("FAILED: " + (totalSize - successCount) + "/" + totalSize);
            }else {
                throw new FileNotFoundException("!Folder is empty - nothing to upload");
            }
        }else {
            throw new FileNotFoundException("!No such folder: " + folderPath);
        }

    }

    private static void processImageName(String imageName) throws InvalidImageExtensionException {
        if (!imageName.matches(REGEX_IMAGE_NAME)){
            throw new InvalidImageExtensionException("!The picture "  + "\"" + imageName + "\"" + " has an invalid extension. Use jpg, jpeg or png.");
        }
    }

    private static void uploadImage(File file, String tagValue){
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(file.getName())
                .tagging(Tagging.builder()
                        .tagSet(new ArrayList<>(Collections.singletonList(Tag.builder()
                                .key(TAG_KEY)
                                .value(tagValue)
                                .build())))
                        .build())
                .build();

        s3Client.putObject(objectRequest, RequestBody.fromFile(file));
    }

    private static void prepareBucket(){
        System.out.println("Bucket preparing...");
        ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
        ListBucketsResponse listBucketsResponse = s3Client.listBuckets(listBucketsRequest);

        Optional<Bucket> searchedBucket = listBucketsResponse.buckets().stream()//bucket name from Amazon
                .filter(n -> n.name().equals(BUCKET_NAME))
                .findAny();

        if (!searchedBucket.isPresent()){
            createBucket(s3Client, AWS_REGION);
        }else {
            System.out.println("Your bucket name: " + BUCKET_NAME);
        }
    }

    // Create a bucket by using a S3Waiter object
    private static void createBucket(S3Client s3, Region region) {
        try {
            S3Waiter s3Waiter = s3Client.waiter();
            CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
                    .bucket(BUCKET_NAME)
                    .createBucketConfiguration(
                            CreateBucketConfiguration.builder()
                                    .locationConstraint(region.id())
                                    .build())
                    .build();

            s3.createBucket(bucketRequest);
            HeadBucketRequest bucketRequestWait = HeadBucketRequest.builder()
                    .bucket(BUCKET_NAME)
                    .build();

            // Wait until the bucket is created and print out the response
            WaiterResponse<HeadBucketResponse> waiterResponse = s3Waiter.waitUntilBucketExists(bucketRequestWait);
            //waiterResponse.matched().response().ifPresent(System.out::println);
            waiterResponse.matched().response().ifPresent(n-> System.out.println());
            System.out.println("Bucket name: " + "\"" + BUCKET_NAME + "\"" +" is ready\n");
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }

}
