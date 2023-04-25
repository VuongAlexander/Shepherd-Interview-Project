package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.BalanceHistory;
import com.shepherdmoney.interviewproject.model.CreditCard;
import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.CreditCardRepository;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.vo.response.CreditCardView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
public class CreditCardController {

    // TODO: wire in CreditCard repository here (~1 line)
    @Autowired
    CreditCardRepository creditCardRepo;

    @Autowired
    UserRepository userRepo;

    @PostMapping("/credit-card")
    public ResponseEntity<String> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        // TODO: Create a credit card entity, and then associate that credit card with user with given userId
        //       Return 200 OK with the credit card id if the user exists and credit card is successfully associated with the user
        //       Return other appropriate response code for other exception cases
        //       Do not worry about validating the card number, assume card number could be any arbitrary format and length
        //Retrieve userId
        Optional<User> optionalUser = userRepo.findById(payload.getUserId());
        if (!optionalUser.isPresent()) {
            return ResponseEntity.notFound().build(); // User not found
        }
        //Create a new CreditCard with issuance bank and card number parameters filled in JSON
        //Set creditCards ownerId from optionalUsers userId
        CreditCard creditCard = new CreditCard(payload.getCardIssuanceBank(), payload.getCardNumber());
        creditCard.setOwner(optionalUser.get());

        //Save creditCard to database and return it with the creditCard Id
        CreditCard savedCreditCard = creditCardRepo.save(creditCard);

        return ResponseEntity.ok("returned credit card to user with ID: " + savedCreditCard.getId());
    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        // TODO: return a list of all credit card associated with the given userId, using CreditCardView class
        //       if the user has no credit card, return empty list, never return null
        // Retrieve the user from the database
        Optional<User> optionalUser = userRepo.findById(userId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            // Retrieve all credit cards associated with the user
            List<CreditCard> creditCards = user.getCreditCards();
            List<CreditCardView> creditCardViews = new ArrayList<>();

            for (CreditCard c : creditCards) {
                CreditCardView creditCardView = new CreditCardView(c.getIssuanceBank(), c.getNumber());
                creditCardViews.add(creditCardView);
            }
            // Return the list of CreditCardView objects as the response
            return ResponseEntity.ok(creditCardViews);
        }else {
            return ResponseEntity.notFound().build(); // User not found
        }
    }

    @GetMapping("/credit-card:user-id")
    public ResponseEntity<String> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        // TODO: Given a credit card number, efficiently find whether there is a user associated with the credit card
        //       If so, return the user id in a 200 OK response. If no such user exists, return 400 Bad Request
        //Retrieve Credit Card number from the database
        Optional<CreditCard> number = creditCardRepo.findByNumber(creditCardNumber);
        if(number.isPresent()){
            //Retrieve credit cards ownerID
            CreditCard creditCard = number.get();
            User userId = creditCard.getOwner();
            return ResponseEntity.ok("returned UserID for Credit Card: " + userId.getId());
        }else{
            return ResponseEntity.notFound().build(); //User not found
        }
    }

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<String> updateCreditCardBalance(@RequestBody UpdateBalancePayload[] payload) {
        //TODO: Given a list of transactions, update credit cards' balance history.
        //      For example: if today is 4/12, a credit card's balanceHistory is [{date: 4/12, balance: 110}, {date: 4/10, balance: 100}],
        //      Given a transaction of {date: 4/10, amount: 10}, the new balanceHistory is
        //      [{date: 4/12, balance: 120}, {date: 4/11, balance: 110}, {date: 4/10, balance: 110}]
        //      Return 200 OK if update is done and successful, 400 Bad Request if the given card number
        //        is not associated with a card.

        for (UpdateBalancePayload updateBalancePayload: payload){
            Optional<CreditCard> creditCard = creditCardRepo.findByNumber(updateBalancePayload.getCreditCardNumber());
            if (creditCard.isPresent()){
                List<BalanceHistory> balanceHistoryList = creditCard.get().getBalanceHistory();
                ZonedDateTime zonedDateTime = updateBalancePayload.getTransactionTime().atZone(ZoneId.systemDefault());

                //Find index of existing Balance History entry with same data as transaction
                int index = -1;
                for (int i = 0; i < balanceHistoryList.size(); i++){
                    if (balanceHistoryList.get(i).getDate().equals(zonedDateTime.toInstant()));
                    index = i;
                    break;
                }
                //If there is no existing Balance history with same data as transaction. Create one
                if (index == -1){
                    BalanceHistory newBalanceHistory = new BalanceHistory();
                    newBalanceHistory.setDate(updateBalancePayload.getTransactionTime());
                    newBalanceHistory.setBalance(updateBalancePayload.getTransactionAmount());
                    newBalanceHistory.setCreditCard(creditCard.get());
                    balanceHistoryList.add(newBalanceHistory);
                }else{
                    balanceHistoryList.get(index).setBalance(updateBalancePayload.getTransactionAmount());
                }
                balanceHistoryList.sort((o1, o2) -> o2.getDate().compareTo(o1.getDate()));
                creditCard.get().setBalanceHistory(balanceHistoryList);
                creditCardRepo.save(creditCard.get());
            }else{
                return ResponseEntity.badRequest().body("Credit Card not found");
            }
        }
        return ResponseEntity.ok().body("Update is done and successful.");
    }

}
