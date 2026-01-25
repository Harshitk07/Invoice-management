package model;

public final class CompanyProfile {

    // Identity
    private final String legalName;
    private final String tradeName;
    private final String description;
    private final String gstin;
    private final String address;
    private final String email;
    private final String phoneNo;

    // Banking
    private final String bankName;
    private final String accountNo;
    private final String ifsc;

    public CompanyProfile(
            String legalName,
            String tradeName,
            String description,
            String gstin,
            String address,
            String email,
            String phoneNo,
            String bankName,
            String accountNo,
            String ifsc
    ) {
        this.legalName = legalName;
        this.tradeName = tradeName;
        this.description = description;
        this.gstin = gstin;
        this.address = address;
        this.email = email;
        this.phoneNo = phoneNo;
        this.bankName = bankName;
        this.accountNo = accountNo;
        this.ifsc = ifsc;
    }

    public String getLegalName() { return legalName; }
    public String getTradeName() { return tradeName; }
    public String getDescription() { return description; }
    public String getGstin() { return gstin; }
    public String getAddress() { return address; }
    public String getEmail() { return email; }
    public String getPhoneNo() { return phoneNo; }

    public String getBankName() { return bankName; }
    public String getAccountNo() { return accountNo; }
    public String getIfsc() { return ifsc; }
}
