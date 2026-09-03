/*
 ^ TUTORIAL 15 — the two roles this API knows

 ? Kept deliberately small: USER (a seller: manages their own books,
 ? tutorial 16) and ADMIN (staff: manages everything). Authorization
 ? design rule: default to the LEAST power; ADMIN is handed out by
 ? other admins, never self-assigned through register.
*/
package com.example.bookshop.model;

public enum Role {
	USER,
	ADMIN
}
