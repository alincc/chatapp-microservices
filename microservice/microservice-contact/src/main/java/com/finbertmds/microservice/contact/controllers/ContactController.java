package com.finbertmds.microservice.contact.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.finbertmds.microservice.contact.models.Contact;
import com.finbertmds.microservice.contact.models.User;
import com.finbertmds.microservice.contact.payload.request.AddUserContactRequest;
import com.finbertmds.microservice.contact.payload.response.MessageResponse;
import com.finbertmds.microservice.contact.services.ContactService;
import com.finbertmds.microservice.contact.services.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ContactController {

  @Autowired
  private UserService userService;

  @Autowired
  private ContactService contactService;

  @GetMapping("/contacts")
  public ResponseEntity<List<Contact>> getContacts(@RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    try {
      List<Contact> contacts = new ArrayList<Contact>();
      Pageable paging = PageRequest.of(page, size);

      contactService.findAll(paging).forEach(contacts::add);

      if (contacts.isEmpty()) {
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
      }

      return new ResponseEntity<>(contacts, HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("/contacts/search")
  public ResponseEntity<List<Contact>> searchContact(@RequestParam(value = "query") String query,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    try {
      List<Contact> contacts = new ArrayList<Contact>();
      Pageable paging = PageRequest.of(page, size);

      if (query == null) {
        contactService.findAll(paging).forEach(contacts::add);
      } else {
        contactService.search(query, paging).forEach(contacts::add);
      }

      if (contacts.isEmpty()) {
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
      }

      return new ResponseEntity<>(contacts, HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  // @PostMapping("/contacts")
  // public ResponseEntity<?> createContact(@RequestBody CreateContactRequest
  // createContactRequest) {
  // if (contactService.existsByEmail(createContactRequest.getEmail())
  // || contactService.existsByUsername(createContactRequest.getUsername())) {
  // return ResponseEntity.badRequest().body(new MessageResponse("Error: Username
  // or Email is existed!"));
  // }
  // try {
  // Contact contact = new Contact();
  // contact.setUsername(createContactRequest.getUsername());
  // contact.setEmail(createContactRequest.getEmail());
  // contact.setFirstName(createContactRequest.getFirstName());
  // contact.setLastName(createContactRequest.getLastName());
  // contact.setPhone(createContactRequest.getPhone());
  // contactService.save(contact);
  // return ResponseEntity.ok(new MessageResponse("Create contact
  // successfully!"));
  // } catch (Exception e) {
  // return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
  // }
  // }

  @GetMapping("/users/{userId}/contacts")
  public ResponseEntity<List<Contact>> getContactsOfUser(@PathVariable("userId") Long userId) {
    try {
      Optional<User> userData = userService.findById(userId);
      if (userData.isEmpty()) {
        return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
      }
      List<Contact> contacts = new ArrayList<Contact>(userData.get().getContacts());
      if (contacts.isEmpty()) {
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
      }

      return new ResponseEntity<>(contacts, HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("/users/{userId}/contacts")
  public ResponseEntity<?> createContactForUser(@PathVariable("userId") Long userId,
      @RequestBody AddUserContactRequest addUserContactRequest) {
    if (StringUtils.isEmpty(addUserContactRequest.getEmail())
        && StringUtils.isEmpty(addUserContactRequest.getUsername())) {
      return ResponseEntity.badRequest().body(new MessageResponse("Error: Username or Email are required!"));
    }
    try {
      Optional<User> userData = userService.findById(userId);
      if (userData.isEmpty()) {
        return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
      }

      Contact contact = null;
      if (contactService.existsByEmail(addUserContactRequest.getEmail())) {
        contact = contactService.findByEmail(addUserContactRequest.getEmail()).get();
      } else {
        if (contactService.existsByUsername(addUserContactRequest.getUsername())) {
          contact = contactService.findByUsername(addUserContactRequest.getUsername()).get();
        } else {
          return ResponseEntity.badRequest().body(new MessageResponse("Error: Contact is not found!"));
        }
      }

      User user = userData.get();
      Set<Contact> contacts = user.getContacts();
      if (contacts.contains(contact)) {
        return ResponseEntity.badRequest().body(new MessageResponse("Error: Contact is exist in User!"));
      }
      contacts.add(contact);
      user.setContacts(contacts);
      userService.save(user);
      return ResponseEntity.ok(new MessageResponse("Add user contact successfully!"));
    } catch (Exception e) {
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
