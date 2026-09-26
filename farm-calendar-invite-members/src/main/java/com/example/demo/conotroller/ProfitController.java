package com.example.demo.conotroller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class ProfitController {


    private final SalesController salesController;

    private final ExpensesController expensesController;



    public ProfitController(
            SalesController salesController,
            ExpensesController expensesController) {

        this.salesController = salesController;
        this.expensesController = expensesController;

    }



    @GetMapping("/profit")
    public String profit(Model model) {


        double salesTotal =
                salesController.getTotal();


        double expenseTotal =
                expensesController.getTotal();



        model.addAttribute(
                "sales",
                salesController.getSales()
        );


        model.addAttribute(
                "expenses",
                expensesController.getExpenses()
        );



        model.addAttribute(
                "salesTotal",
                salesTotal
        );


        model.addAttribute(
                "expenseTotal",
                expenseTotal
        );


        model.addAttribute(
                "profit",
                salesTotal - expenseTotal
        );



        return "profit";

    }

}