

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class StockDataFetcher
{
	static Logger logger = Logger.getLogger("StockDataFetcher");
	static String authToken = null; 
	static ArrayList<FileDownLoadParams> packageDeliveryIdList = new ArrayList<FileDownLoadParams>();
	static ArrayList<String> subscriptionIdList = new ArrayList<String>();
	final static Properties prop = new Properties();
	static Calendar cal = Calendar.getInstance();
	static String startTime="";
	static ArrayList<ReportLineVenueList> venueLines = new ArrayList<ReportLineVenueList>();
	static ArrayList<FileDownLoadParams> fileListLines = new ArrayList<FileDownLoadParams>();
	static ArrayList<String> exInstrumentFolders = new ArrayList<String>();
	static PrintWriter writer;
	static String fromDate=null;
	static String toDate=null;
	static String reportName=null;
	public static void main(String[] args)
	{
		try
		{
			Runtime.getRuntime().addShutdownHook(new Thread()
			{
				@Override
				public void run()
				{
					logger.info("Application exiting.Performing clean up");
				}
			});
			startTime=cal.getTime().toString();
			logger.info("Starting Application..");
			//Read config data from config file
			if(args.length > 0)
			{
				logger.info("Using config file:"+args[0]);
				prop.load(new FileInputStream(args[0]));
				if(args.length == 2 && args[1] != null)
				{

					try
					{
						fromDate = LocalDate.parse(args[1]).toString()+"T00:00:01.000Z";
						toDate = LocalDate.parse(args[1]).plusDays(1).toString()+"T00:00:01.000Z";
					}
					catch(DateTimeParseException e)
					{
						logger.fatal("Date param should be in YYYY-MM-DD format.Aborting program");
						System.exit(-1);
					}
				}
				else
				{
					fromDate = LocalDate.now().minusDays(1L).toString()+"T00:00:01.000Z";
					toDate = LocalDate.now().toString()+"T00:00:01.000Z";
				}
				logger.info("Parsed Property File");
				logger.info("Checking environment setup..");
				if(checkConfig()) //This is to ensure that the data directories and required parameters are set in the config file
				{
					logger.info("Environment Check OK..");
					if(authToken == null)
						getAuthToken(); //Authorization token is created at this stage and then reused for subsequent calls within the same program
					getUserPackages(); //get the list of Venue files to download
					for(int i=0;i<subscriptionIdList.size();i++)
					{
						getFilesForDateRange(subscriptionIdList.get(i)); //All files for that particular subscription id and date range (T-1) are fetched 
					}
					for(int i=0;i<packageDeliveryIdList.size();i++)
					{
						downloadFileByPackageDeliveryId(packageDeliveryIdList.get(i));
					}
					initializeReport(startTime,Calendar.getInstance().getTime().toString()); //Report to write the details of the job
					if(venueLines.size() > 0)
					{
						printRowVenueHeader();
						Iterator<ReportLineVenueList> iterator = venueLines.iterator();
						int i=1;
						while (iterator.hasNext()){
							ReportLineVenueList item = iterator.next();
							printRowVenue(i,item.getPackageName(),item.getSubscriptionId());
							exInstrumentFolders.add(item.getPackageName().substring(0, 3));
							i++;
						}
						printRowVenueFooter();
					}
					writer.flush();
					if(fileListLines.size() > 0)
					{
						printRowFileParamHeader();
						int i=1;
						Iterator<FileDownLoadParams> iterator = fileListLines.iterator();
						while (iterator.hasNext()){
							FileDownLoadParams item = iterator.next();
							printRowFileParam(i,item.getFileName(),item.getSubscriptionId(),item.getReleaseDateTime(),item.getFileSize(),item.getFrequency(),item.getChecksum(),verifyDownload(item.getFileName(),item.getChecksum()));
							i++;
						}
						printRowFileParamFooter();
					}
					writer.flush();
					writer.close();
				}
			}
			else
			{
				logger.fatal("Usage:StockDataFetcher [ConfigFile]");
				System.exit(-1);
			}

		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
		catch ( Exception e)
		{
			e.printStackTrace();
		}
		if(exInstrumentFolders.size() > 0)
		{
			Iterator<String> iterator = exInstrumentFolders.iterator();
			String rptFileName=reportName.substring(reportName.lastIndexOf("/"));
			while (iterator.hasNext()){
				String item = iterator.next();
				try {
					Files.copy(new File(reportName).toPath(), new File(prop.getProperty("data_download_dir")+"/"+item+rptFileName).toPath(),StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	//Parse JSON response in this function
	public static JSONArray parseResponseJSON(String json,Integer type) throws ApplicationException
	{
		String result=null;
		try {
			JSONParser parser = new JSONParser();
			Object resultObject = parser.parse(json);

			if (resultObject instanceof JSONObject) {
				JSONObject obj =(JSONObject)resultObject;
				result = obj.get("value").toString();
				if(type==1)
				{
					authToken=result;
				}
				if(type==2)
				{	
					JSONArray msg = (JSONArray) obj.get("value");
					return msg;
				}
			}
			else
				throw new ApplicationException("Invalid Datatype returned for request");

		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}	
	//Pass the correct headers to get Authorization Token
	public static void getAuthToken()
	{
		String body = "{ \"Credentials\": { \"Username\": \"" + prop.getProperty("username") + "\", \"Password\": \"" + prop.getProperty("password") + "\"}}";
		try {
			parseResponseJSON(httpPost(prop.getProperty("authUrl"),body),1);
			logger.info("Obtained Authorization token successfully");
			logger.debug("Auth Token:"+authToken);
		} catch (ApplicationException e) {
			logger.error(e.getMessage());
		}
	}
	//Get available venues for user
	public static void getUserPackages()
	{
		JSONArray respArray = new JSONArray();
		try {
			respArray = parseResponseJSON(httpGet(prop.getProperty("venuePackageIdUrl")),2);
			logger.info("Getting list of Venues");
			//Each venue has a subscription id, which is then used to determine the list of files available. This subscription is stored in a list and used later
			for(int i=0;i < respArray.size();i++)
			{
				JSONObject respObj = new JSONObject();
				respObj = (JSONObject) respArray.get(i);
				ReportLineVenueList venueList = new ReportLineVenueList(respObj.get("UserPackageId").toString(),respObj.get("PackageId").toString(),respObj.get("PackageName").toString(),respObj.get("SubscriptionId").toString(),respObj.get("SubscriptionName").toString());
				venueLines.add(venueList);
				if(!subscriptionIdList.contains(respObj.get("SubscriptionId")))
					subscriptionIdList.add(respObj.get("SubscriptionId").toString());
			}
			logger.info("Venue List Obtained : "+respArray.size());

		} catch (ApplicationException e) {
			logger.error(e.getMessage());
		}
	}
	private static void getFilesForDateRange(String string) {
		JSONArray respArray = new JSONArray();
		String url = prop.getProperty("fileListForDateRange");

		LocalDate.now(ZoneId.of(prop.getProperty("timeZone")));

		url = url.replaceAll("<SUBSCR_ID>", string);

		url = url.replaceAll("<FROM_DATE>", fromDate);
		url = url.replaceAll("<TO_DATE>", toDate);
		logger.info("Obtaining files between " + fromDate + " and " + toDate);
		logger.debug("URL " + url);

		try {
			respArray = parseResponseJSON(httpGet(url),2);
			logger.info("List of Files to download");
			for(int i=0;i < respArray.size();i++)
			{
				JSONObject respObj = new JSONObject();
				respObj = (JSONObject) respArray.get(i);
				FileDownLoadParams fileParam = new FileDownLoadParams(respObj.get("PackageDeliveryId").toString(),respObj.get("UserPackageId").toString(),respObj.get("SubscriptionId").toString(),respObj.get("Name").toString(),respObj.get("ReleaseDateTime").toString(),(Long) respObj.get("FileSizeBytes"),respObj.get("Frequency").toString(),respObj.get("ContentMd5").toString());
				fileListLines.add(fileParam);
				if(!packageDeliveryIdList.contains(fileParam))
					packageDeliveryIdList.add(fileParam);
			}
			logger.info("Files to download : "+respArray.size());

		} catch (ApplicationException e) {
			logger.error(e.getMessage());
		}

	}
	//Files are downloaded here
	private static void downloadFileByPackageDeliveryId(FileDownLoadParams params) {
		String url = prop.getProperty("fileDownloadUrl");
		url = url.replaceAll("<PKG_DELIVERY_ID>", params.getPackageDeliveryId());
		logger.debug("File URL" + url);
		logger.info("Downloading File " + params.getFileName());

		try {
			httpDownload(url,params.getFileName());
		} catch (ApplicationException e) {
			logger.error(e.getMessage());
		} 	
	}
	public static String httpPost(String url, String body) throws ApplicationException {

		String response = null;
		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
			HttpPost request = new HttpPost(url);
			request.addHeader("content-type", "application/json");
			StringEntity params = new StringEntity(body);
			request.setEntity(params);
			HttpResponse result = httpClient.execute(request);
			if(result.getStatusLine().toString().contains("200"))
				response = EntityUtils.toString(result.getEntity(), "UTF-8");
			else
				throw new ApplicationException("Error during HttpPost Call.Please make sure the correct URL is being called.");

		} catch (IOException ex) {
		}
		return response;
	}
	public static String httpGet(String url) throws ApplicationException {

		String returnData = null;
		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
			HttpGet request = new HttpGet(url);
			request.addHeader("content-type", "application/json");
			request.addHeader("Authorization", "Token "+authToken);
			if(prop.containsKey("user_agent"))
			{
				logger.info("Overriding user-agent to "+prop.getProperty("user_agent"));
				request.setHeader("User-Agent", prop.getProperty("user_agent"));
			}
			else
				request.setHeader("User-Agent", "PostmanRuntime/7.1.1");
			
			CloseableHttpResponse response = httpClient.execute(request);
			if(response.getStatusLine().toString().contains("200"))
			{	
				returnData = EntityUtils.toString(response.getEntity(), "UTF-8");
			}
			else
			{
				throw new ApplicationException("Error during HttpGet Call.Please make sure the correct URL is being called");
			}


		} catch (IOException ex) {
			logger.error(ex.getMessage());
		}
		return returnData;
	}
	public static String httpDownload(String url,String fileName) throws ApplicationException {

		String returnData = null;
		String folderName = null;
		OutputStream out = null;

		folderName = getFileDir(fileName);

		try (CloseableHttpClient httpClient = HttpClients.custom().disableContentCompression().build()) 
		{

			new File(prop.getProperty("data_download_dir")+"/"+ folderName).mkdirs();
			out = new FileOutputStream(prop.getProperty("data_download_dir")+"/"+ folderName + "/" + fileName);

			HttpGet request = new HttpGet(url);
			request.addHeader("content-type", "application/json");
			request.addHeader("Authorization", "Token "+authToken);
			if(prop.containsKey("direct_download") && prop.getProperty("direct_download").equalsIgnoreCase("true"))
			{
				logger.debug("Direct Download ON");
				request.addHeader("X-Direct-Download","true");
			}
			request.setHeader(HttpHeaders.ACCEPT_ENCODING, "gzip");

			CloseableHttpResponse response = httpClient.execute(request);
			if(response.getStatusLine().toString().contains("200"))
			{	
				try 
				{
					HttpEntity entity = response.getEntity();
					entity.writeTo(out);
				} 
				finally {
					response.close();
					out.close();
				}
			}
			else
			{
				out.close();
				throw new ApplicationException("Error during HttpGet Call.Please make sure the correct URL is being called");
			}
		}
		catch (IOException ex) {
			logger.error(ex.getMessage());
		}
		return returnData;
	}
	private static String getFileDir(String fileName) {
		String dirMidLevel="";
		String dirPrefix=fileName.substring(0,3);
		if(fileName.contains("Instruments"))
			dirMidLevel="Instruments";
		else
			dirMidLevel="TAS";
		String dateFolder=fileName.substring(4,8)+"/"+fileName.substring(9,11)+"/"+fileName.substring(12,14);
		logger.debug("Dir for file " + fileName + " is "+dirPrefix+"/"+dirMidLevel +"/"+ dateFolder);
		return dirPrefix+"/"+dirMidLevel +"/"+ dateFolder;
	}
	public static String verifyDownload(String fileName,String chkSum)
	{
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			FileInputStream fis = new FileInputStream(prop.getProperty("data_download_dir")+"/"+getFileDir(fileName)+"/"+fileName);

			byte[] dataBytes = new byte[1024];

			int nread = 0;
			while ((nread = fis.read(dataBytes)) != -1) {
				md.update(dataBytes, 0, nread);
			};
			byte[] mdbytes = md.digest();
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < mdbytes.length; i++) {
				sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			fis.close();
			logger.debug(fileName+" MD5 =  " + sb.toString());
			if(chkSum.length() > 1)
			{
				if(sb.toString().equals(chkSum))
				{
					return "Checksums match.Verfied OK";
				}
				else
				{
					return "Checksums do NOT match";
				}
			}
			else
				return "No Source MD5 to verify";

		} catch (NoSuchAlgorithmException e) {
			logger.error(e.getMessage());
			return(e.getMessage());
		} catch (IOException e) {
			logger.error(e.getMessage());
			return("File Missing.Unable to verify");
		}
	}

	public static boolean checkConfig()
	{
		if(checkDirectory("data_download_dir")
				&& checkDirectory("reports_dir") 
				&& checkConfigParam("username")
				&& checkConfigParam("password")
				&& checkConfigParam("authUrl")
				&& checkConfigParam("venuePackageIdUrl")
				&& checkConfigParam("fileListForDateRange")
				&& checkConfigParam("fileDownloadUrl"))
			return true;
		else
			return false;
	}
	public static boolean checkDirectory(String dirName)
	{
		if(prop.getProperty(dirName)==null)
		{
			logger.fatal("Property "+dirName+" has not been defined in the config file. Cannot proceed");
			System.exit(-1);
		}
		dirName=prop.getProperty(dirName);
		logger.info("Checking if path " + dirName + " exists.");
		File f = new File(dirName);
		if(!f.exists() && !f.isDirectory())
		{
			logger.warn(dirName + " does not exist.Attempting to create");
			if(!f.mkdir())
			{
				logger.fatal("Could not create directory " + dirName + ". Please check if you have the required permissions. Program will now exit");
				System.exit(-1);
				return false;
			}
			else
				logger.info(dirName + " created successfuly.");
		}
		else
		{
			logger.info(dirName + " already exists.OK.");
		}
		return true;
	}
	public static boolean checkConfigParam(String paramName)
	{
		if(prop.getProperty(paramName)==null)
		{
			logger.fatal("Property "+paramName+" has not been defined in the config file. Cannot proceed");
			System.exit(-1);
		}
		return true;
	}
	public static void initializeReport(String startTime, String endTime) {
		DateFormat dateFormat = new SimpleDateFormat("ddMMYYYY-HH_mm");
		Calendar cal = Calendar.getInstance();
		reportName=prop.getProperty("reports_dir")+"/ReutersStockFeedFetch_"+dateFormat.format(cal.getTime())+".rpt";
		try {
			writer = new PrintWriter(reportName, "UTF-8");
			writer.println("**************************************************** Reuters Stock Feed Fetcher ***************************************************");
			writer.println("Program Start Time :"+startTime);
			writer.println("Program End Time   :"+endTime);
			writer.println("***********************************************************************************************************************************");
			writer.println(" ");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	static void printRowVenueHeader() {
		writer.println("List Of Venues Available for Subscription");
		writer.println(" ");
		writer.printf("%-5s %-60s %-25s%n", "-----", "-----------------------------------------------------------","-------------------------");
		writer.printf("%-5s %-60s %-25s%n", "S.No", "Package Name","Subscription Id");
		writer.printf("%-5s %-60s %-25s%n", "-----", "-----------------------------------------------------------","-------------------------");
	}
	static void printRowVenueFooter() {
		writer.printf("%-5s %-60s %-25s%n", "-----", "-----------------------------------------------------------","-------------------------");
		writer.println(" ");
	}
	static void printRowVenue(Object obj1,Object obj2,Object obj3) {
		writer.printf("%-5s %-60s %-25s%n", obj1.toString(), obj2.toString(),obj3.toString());
	}
	static void printRowFileParamHeader() {
		writer.println(" ");
		writer.println("List Of Files Available for Download");
		writer.println(" ");
		writer.printf("%-5s %-60s %-25s %-25s %-15s %-10s %-35s %-25s%n", "-----", "------------------------------------------------------------","-------------------------","-------------------------","---------------","----------","-----------------------------------","-------------------------");
		writer.printf("%-5s %-60s %-25s %-25s %-15s %-10s %-35s %-25s%n", "S.No", "File Name","Subscription Id","Release Date/Time","FileSize(Bytes)","Frequency","MD5 Checksum","Download Status");
		writer.printf("%-5s %-60s %-25s %-25s %-15s %-10s %-35s %-25s%n", "-----", "------------------------------------------------------------","-------------------------","-------------------------","---------------","----------","-----------------------------------","-------------------------");
	}
	static void printRowFileParamFooter() {
		writer.printf("%-5s %-60s %-25s %-25s %-15s %-10s %-35s %-25s%n", "-----", "------------------------------------------------------------","-------------------------","-------------------------","---------------","----------","-----------------------------------","-------------------------");
		writer.println(" ");
	}
	static void printRowFileParam(Object obj1,Object obj2,Object obj3,Object obj4,Object obj5,Object obj6,Object obj7,Object obj8) {
		writer.printf("%-5s %-60s %-25s %-25s %-15s %-10s %-35s %-25s%n", obj1.toString(), obj2.toString(),obj3.toString(),obj4.toString(),obj5.toString(),obj6.toString(),obj7.toString(),obj8.toString());
	}
}