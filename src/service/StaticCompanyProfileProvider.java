package service;

import model.CompanyProfile;

public final class StaticCompanyProfileProvider
        implements CompanyProfileProvider {

    @Override
    public CompanyProfile loadProfile() {
        return new CompanyProfile(
                "SHREE UMA ASSOCIATES",
                "",
                "Govt. Order Supply &amp; Contractors",
                "37AFPTK3972K1ZN",
                "Door No. 58-3-10, Ramanaidu Colony",
                "ashokekumar122@example.com",
                "8330912353",
                "Punjab National Bank",
                "4481002100003591",
                "PUNB0448100"

        );
    }
}
