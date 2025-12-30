# InvEx: Advanced Inventory Management Application

### Developers
Khashayar Khosrosourmi 22302442

Sahand Mohammadi 22115582

## Introduction

InvEx is an inventory management application built using **JavaFX** and **Hibernate ORM**, providing a simple yet efficient and user-friendly interface for managing inventory.

The project uses the **Liberica Full JDK 21**, which can be downloaded automatically by **IntelliJ IDEA** when opening the project.

## Running the Application

To run the application, open `InvexApplication.java` located at:

src/main/java/com/invex/invex

Run the file to start the application.

When the application is run for the first time, the **database**, the **admin user**, and the **inventory object** are created automatically.

## Login Credentials

When prompted to log in, use the following credentials:

- **Username:** admin
- **Password:** admin

## Using the Application

After logging in, you will see the main application window.

At the **top left** of the window, there are buttons for **creating**, **editing**, and **deleting** products.  
At the **top right** of the header, the **total inventory value** is displayed and updated automatically.

You can also create **transactions** by using the provided button. Available transaction types include **sales** and **restock**.  
Once a transaction is created, it will appear in the **transaction history** section at the bottom of the page.

Each transaction includes an **Undo** button, allowing you to revert it if needed.

Please note that the application **prevents deleting a product** if it has any associated transactions.