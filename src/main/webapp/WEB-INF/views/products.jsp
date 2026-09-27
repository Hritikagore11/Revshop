<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html>
<html>
<head>
    <title>RevShop - Products</title>

    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 30px;
        }

        h1 {
            margin-bottom: 20px;
        }

        .product {
            border: 1px solid #ddd;
            padding: 15px;
            margin-bottom: 15px;
            border-radius: 8px;
        }

        .price {
            font-weight: bold;
        }

        .discount {
            color: green;
        }
    </style>
</head>

<body>

<h1>RevShop Products</h1>

<c:forEach var="product" items="${products}">

    <div class="product">

        <h2>${product.name}</h2>

        <p>${product.description}</p>

        <p class="price">
            Price: ₹${product.price}
        </p>

        <c:if test="${product.discount > 0}">
            <p class="discount">
                Discount: ${product.discount}%
            </p>

            <p class="price">
                Final Price: ₹${product.discountedPrice}
            </p>
        </c:if>

        <p>
            Stock: ${product.quantity}
        </p>

        <p>
            Seller ID: ${product.sellerId}
        </p>

        <p>
            Category: ${product.category.name}
        </p>

    </div>

</c:forEach>

</body>
</html>