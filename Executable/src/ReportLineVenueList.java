

public class ReportLineVenueList {
    public String userPackageId = null;
    public String packageId = null;
    public String packageName = null;
    public String subscriptionId = null;
    public String subscriptionName = null;
	public String getUserPackageId() {
		return userPackageId;
	}
	public void setUserPackageId(String userPackageId) {
		this.userPackageId = userPackageId;
	}
	public String getPackageId() {
		return packageId;
	}
	public void setPackageId(String packageId) {
		this.packageId = packageId;
	}
	public String getPackageName() {
		return packageName;
	}
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}
	public String getSubscriptionId() {
		return subscriptionId;
	}
	public void setSubscriptionId(String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}
	public String getSubscriptionName() {
		return subscriptionName;
	}
	public void setSubscriptionName(String subscriptionName) {
		this.subscriptionName = subscriptionName;
	}
	public ReportLineVenueList(String userPackageId, String packageId, String packageName, String subscriptionId,
			String subscriptionName) {
		super();
		this.userPackageId = userPackageId;
		this.packageId = packageId;
		this.packageName = packageName;
		this.subscriptionId = subscriptionId;
		this.subscriptionName = subscriptionName;
	}
    
}
