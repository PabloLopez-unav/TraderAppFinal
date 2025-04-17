let selectedStock = null;

document.addEventListener('DOMContentLoaded', async () => {
    await cargarCartera();
});

async function cargarCartera() {
    const tablaCartera = document.getElementById('tablaCartera');
    tablaCartera.innerHTML = "<p>Cargando tu cartera...</p>";

    try {
        const response = await fetch('Vender');
        if (!response.ok) throw new Error("Error al cargar la cartera");

        const data = await response.json();

        if (data.length === 0) {
            tablaCartera.innerHTML = "<p>No tienes acciones en tu cartera.</p>";
            return;
        }

        let html = `
            <table>
                <tr>
                    <th>Seleccionar</th>
                    <th>Nombre</th>
                    <th>Acciones</th>
                    <th>Precio Compra</th>
                    <th>Fecha</th>
                    <th>Valor Actual</th>
                </tr>
        `;

        const stocks = data.map(item => item.StockName);
        const currentPrices = await obtenerPreciosActuales(stocks);

        data.forEach((item, index) => {
            const currentPrice = currentPrices[item.StockName] || 0;
            const totalValue = currentPrice * item.Num;

            html += `
                <tr>
                    <td><input type="radio" name="accion" value="${item.StockName}" 
                        data-cantidad="${item.Num}" data-precio="${currentPrice}" 
                        onchange="seleccionarAccion(this)"></td>
                    <td>${item.StockName}</td>
                    <td>${item.Num}</td>
                    <td>${item.Price.toFixed(2)}€</td>
                    <td>${item.Date}</td>
                    <td>${totalValue.toFixed(2)}€ (${currentPrice.toFixed(2)}€/acc)</td>
                </tr>
            `;
        });

        html += `</table>`;
        tablaCartera.innerHTML = html;
    } catch (error) {
        tablaCartera.innerHTML = `<p style="color: red">Error al cargar la cartera: ${error.message}</p>`;
    }
}

async function obtenerPreciosActuales(stocks) {
    const prices = {};

    for (const stock of stocks) {
        try {
            let tickerSymbol = stock;
            if (['ACS', 'ANA', 'BBVA', 'BKT', 'ENG', 'AMS', 'ITX'].includes(stock)) {
                tickerSymbol += ".MC";
            }

            const url = `https://corsproxy.io/?https://query1.finance.yahoo.com/v8/finance/chart/${tickerSymbol}?interval=1d&range=1d`;
            const response = await fetch(url);
            const data = await response.json();

            const result = data.chart?.result?.[0];
            if (result) {
                const quote = result.indicators.quote[0];
                prices[stock] = quote.close[quote.close.length - 1];
            }
        } catch (e) {
            console.error(`Error al obtener precio de ${stock}:`, e);
            prices[stock] = 0;
        }
    }

    return prices;
}

function seleccionarAccion(radio) {
    if (radio.checked) {
        selectedStock = {
            symbol: radio.value,
            maxQuantity: parseInt(radio.getAttribute('data-cantidad')),
            currentPrice: parseFloat(radio.getAttribute('data-precio'))
        };

        document.getElementById('ventaSection').classList.remove('hidden');
        document.getElementById('cantidad').max = selectedStock.maxQuantity;
        document.getElementById('cantidad').value = '';
        document.getElementById('mensaje').textContent = '';
    }
}

document.getElementById('venderBtn').addEventListener('click', async () => {
    const cantidad = parseInt(document.getElementById('cantidad').value);
    const mensaje = document.getElementById('mensaje');

    if (!cantidad || cantidad <= 0 || cantidad > selectedStock.maxQuantity) {
        mensaje.textContent = "Introduce una cantidad válida";
        return;
    }

    try {
        const response = await fetch('Vender', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                accion: selectedStock.symbol,
                cantidad: cantidad,
                precio: selectedStock.currentPrice
            })
        });

        const result = await response.json();
        mensaje.textContent = result.message;

        if (result.success) {
            document.getElementById('cantidad').value = '';
            await cargarCartera();
        }
    } catch (error) {
        mensaje.textContent = "Error al realizar la venta";
        console.error(error);
    }
});