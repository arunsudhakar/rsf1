

public class FileDownLoadParams {
    public String packageDeliveryId = null;
    public String userPackageId = null;
    public String subscriptionId = null;
    public String fileName = null;
    public String releaseDateTime = null;
    public long fileSize= 0L;
    public String frequency = null;
    public String checksum = null;
    public Boolean downloadStatus = false;
	
    public String getPackageDeliveryId() {
		return packageDeliveryId;
	}
	public void setPackageDeliveryId(String packageDeliveryId) {
		this.packageDeliveryId = packageDeliveryId;
	}
	public String getUserPackageId() {
		return userPackageId;
	}
	public void setUserPackageId(String userPackageId) {
		this.userPackageId = userPackageId;
	}
	public String getSubscriptionId() {
		return subscriptionId;
	}
	public void setSubscriptionId(String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getReleaseDateTime() {
		return releaseDateTime;
	}
	public void setReleaseDateTime(String releaseDateTime) {
		this.releaseDateTime = releaseDateTime;
	}
	public long getFileSize() {
		return fileSize;
	}
	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}
	public String getFrequency() {
		return frequency;
	}
	public void setFrequency(String frequency) {
		this.frequency = frequency;
	}
	public String getChecksum() {
		return checksum;
	}
	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}
	
	public Boolean getDownloadStatus() {
		return downloadStatus;
	}
	public void setDownloadStatus(Boolean downloadStatus) {
		this.downloadStatus = downloadStatus;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileDownLoadParams other = (FileDownLoadParams) obj;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		return true;
	}
	public FileDownLoadParams(String packageDeliveryId, String userPackageId, String subscriptionId, String fileName,
			String releaseDateTime, long fileSize, String frequency, String checksum) {
		super();
		this.packageDeliveryId = packageDeliveryId;
		this.userPackageId = userPackageId;
		this.subscriptionId = subscriptionId;
		this.fileName = fileName;
		this.releaseDateTime = releaseDateTime;
		this.fileSize = fileSize;
		this.frequency = frequency;
		this.checksum = checksum;
	}
	
    
}
