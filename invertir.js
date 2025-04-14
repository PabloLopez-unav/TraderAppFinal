// Obtener datos de acciones desde Yahoo Finance
let currentData = null;

document.getElementById('consultarBtn').addEventListener('click', async () => {
    const symbol = document.getElementById("acciones").value.toUpperCase();
    const resultadoDiv = document.getElementById('tablaPrecios');
    resultadoDiv.innerHTML = "<p>Cargando datos...</p>";

    let tickerSymbol = symbol;
    if (['ACS', 'ANA', 'BBVA', 'BKT', 'ENG', 'AMS', 'ITX'].includes(symbol)) {
        tickerSymbol += ".MC";
    }

    const url = `https://corsproxy.io/?https://query1.finance.yahoo.com/v8/finance/chart/${tickerSymbol}?interval=1d&range=2mo`;

    try {
        const response = await fetch(url);
        const data = await response.json();

        const result = data.chart?.result?.[0];
        if (!result) throw new Error("Datos no disponibles");

        const quotes = result.indicators.quote[0];
        const timestamps = result.timestamp;
        const closePrices = quotes.close;

        const historyData = timestamps.map((ts, i) => ({
            date: new Date(ts * 1000).toISOString().split('T')[0],
            close: closePrices[i]
        })).slice(-50).reverse();

        currentData = { symbol: tickerSymbol, latestClose: historyData[0].close, history: historyData };

        resultadoDiv.innerHTML = `
            <h3>Evolución de ${symbol} (últimos 50 días)</h3>
            <table>
                <tr><th>Fecha</th><th>Precio de Cierre (€)</th></tr>
                ${historyData.map(day => `<tr><td>${day.date}</td><td>${day.close.toFixed(2)}</td></tr>`).join('')}
            </table>
        `;
        document.getElementById('compraSection').style.display = 'block';
    } catch (error) {
        resultadoDiv.innerHTML = `<p style="color: red">No se pudieron obtener datos.</p>`;
        document.getElementById('compraSection').style.display = 'none';
    }
});

// ==============================================
// CÓDIGO DEL BOTÓN DE COMPRA (NUEVO)
// ==============================================
document.getElementById('comprarBtn').addEventListener('click', async () => {
    const cantidad = parseInt(document.getElementById('cantidad').value);
    const mensaje = document.getElementById('mensaje');

    if (!cantidad || cantidad <= 0) {
        mensaje.textContent = "Introduce una cantidad válida";
        return;
    }

    try {
        const response = await fetch('comprar', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                accion: currentData.symbol.replace('.MC', ''),
                cantidad: cantidad,
                precio: currentData.latestClose
            })
        });

        const result = await response.json();
        mensaje.textContent = result.message;

        if (result.success) {
            document.getElementById('cantidad').value = '';
            mensaje.textContent = "Compra realizada con exito";
        }
    } catch (error) {
        mensaje.textContent = "Error al realizar la compra";
        console.error(error);
    }
});