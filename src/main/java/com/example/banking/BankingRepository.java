package com.example.banking;

import java.io.IOException;

public interface BankingRepository {
    BankingData load() throws IOException;

    void save(BankingData data) throws IOException;
}
