package com.example.ev2

import android.app.Application
import android.content.Context
import android.media.Image
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
        App()
        }
    }
    override fun onStop(){
        super.onStop()
        val viewModel:MainViewModel by viewModels()
        viewModel.guardarListaDeProductos(this)
    }
}
data class Producto(
    val nombre:String,
    var comprado:Boolean
)
object Destinations {
    const val PantallaInicio = "main"
    const val PantallaConfig = "settings"
}
@Composable
fun App() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Destinations.PantallaInicio ){
        composable(Destinations.PantallaInicio){
            PantallaAppCompras(navController)
        }
        composable(Destinations.PantallaConfig){
            PantallaAppConfig(navController)
        }
    }
}
class MainViewModel(application:Application):AndroidViewModel(application){
    private val _productos = MutableLiveData(listOf<Producto>())
    val productos: LiveData<List<Producto>> = _productos
    private val _mostrarPrioridadItems = MutableLiveData(false)
    val mostrarPrioridadItems: LiveData<Boolean> = _mostrarPrioridadItems
    private val _ordenAlfabetico = MutableLiveData(false)
    val ordenAlfabetico: LiveData<Boolean> = _ordenAlfabetico
    init{
        val context = getApplication<Application>().applicationContext
        cargarListaDeProductos(context)
        val ordenAlfabetico = recuperarOrdenAlfabetico(context)
        _ordenAlfabetico.value = ordenAlfabetico
        val mostrarPrioridad = recuperarOrdenPrioridad(context)
        _mostrarPrioridadItems.value = mostrarPrioridad
    }
    fun agregarProducto(producto: Producto) {
        viewModelScope.launch (Dispatchers.Main) {
            val currentList = _productos.value ?: emptyList()
            val updateList = currentList + producto
            _productos.value = if (_ordenAlfabetico.value == true) {
                updateList.sortedBy { it.nombre }
            } else {
                updateList
            }
        }
    }
    fun eliminarProducto(producto: Producto) {
        viewModelScope.launch(Dispatchers.Main) {
            val updatedList = _productos.value?.filterNot { it == producto }
            _productos.value = if (_ordenAlfabetico.value == true){
                updatedList?.sortedBy { it.nombre }
            } else  {
                updatedList
            }
        }
    }
    fun cambiarEstadoProducto(producto: Producto,comprado: Boolean){
        viewModelScope.launch(Dispatchers.Main) {
            val updatedList = _productos.value?.map {if(it==producto) it.copy(comprado=comprado)else it}
            _productos.value = if (_ordenAlfabetico.value == true) {
                updatedList?.sortedBy { it.nombre }
            } else {
                updatedList
            }
        }
    }
    fun setOrdenAlfabetico(orden:Boolean){
        _ordenAlfabetico.value = orden
    }
    fun guardarOrdenAlfabetico(context: Context, orden: Boolean){
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putBoolean("orden_alfabetico",orden).apply()
    }
    fun recuperarOrdenAlfabetico(context: Context):Boolean{
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean("orden_alfabetico",false)
    }
    fun setOrdenPrioridad(mostrar:Boolean){
        _mostrarPrioridadItems.value = mostrar
        ordenarProductos()
    }
    fun guardarOrdenPrioridad(context: Context,mostrar: Boolean){
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        with(prefs.edit()) {
            putBoolean("mostrar_prioridad_items",mostrar)
            apply()
        }
    }
    fun recuperarOrdenPrioridad(context: Context):Boolean{
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean("mostrar_prioridad_items",false)
    }
    private fun ordenarProductos(){
        _productos.value = _productos.value?.let {lista ->
            var sortedList = lista
            if (_ordenAlfabetico.value == true) {
                sortedList=sortedList.sortedBy { it.nombre }
            }
            if (_mostrarPrioridadItems.value==true) {
                sortedList = sortedList.sortedWith(compareBy<Producto> {!it.comprado}.thenBy {it.nombre})
            }
            sortedList
        }
    }
    fun guardarListaDeProductos(context: Context){
        val listaDeProductos = _productos.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val json = Gson().toJson(listaDeProductos)
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.edit().putString("lista_productos",json).apply()
        }
    }
    fun cargarListaDeProductos(context: Context){
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val json = prefs.getString("lista_productos",null)?: return@launch
            val tipoListaProductos = object : TypeToken<List<Producto>>() {}.type
            val listaDeProductos: List<Producto> = Gson().fromJson(json, tipoListaProductos)
            _productos.postValue(listaDeProductos)
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAppCompras(navController: NavController) {
    val viewModel: MainViewModel = viewModel()
    val productos by viewModel.productos.observeAsState(listOf())
    var nuevoProducto by remember { mutableStateOf("") }
    Scaffold(
    ) {innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

        }
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.shopping_list),
                    fontSize = 20.sp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 16.dp)
                )
                IconButton(onClick = {navController.navigate(Destinations.PantallaConfig)}) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(id = R.string.Settings),
                        modifier = Modifier
                            .size(40.dp))
                }
            }
        }
        Column {
            Spacer(modifier = Modifier.height(50.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(100.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            )
            {
                TextField(
                    value = nuevoProducto,
                    placeholder = {Text(stringResource(id = R.string.Product))},
                    onValueChange = {nuevoProducto = it},
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painterResource(id = R.drawable.add),
                    contentDescription = stringResource(id = R.string.add),
                    modifier = Modifier
                        .size(40.dp)
                        .clickable {
                            if (nuevoProducto.isNotEmpty()) {
                                viewModel.agregarProducto(Producto(nuevoProducto, false))
                                nuevoProducto = ""
                            }
                        }
                )
            }
            LazyColumn(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 50.dp)
            ) {
                items(productos) { producto ->
                    var productoTachado by remember { mutableStateOf(producto.comprado) }
                    Column() {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    producto.comprado = !producto.comprado
                                    productoTachado = producto.comprado
                                }
                        ) {
                            Text(
                                producto.nombre,
                                modifier = Modifier
                                    .weight(if (productoTachado) 1f else 2f)
                                    .padding(end = 8.dp),
                                style = if (productoTachado) LocalTextStyle.current.copy(textDecoration = TextDecoration.LineThrough) else LocalTextStyle.current
                            )
                            Row{
                                Checkbox(
                                    checked = producto.comprado,
                                    onCheckedChange = { isChecked ->
                                        viewModel.cambiarEstadoProducto(producto, isChecked)
                                    },
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                                if (producto.comprado) {
                                    Icon(
                                        painterResource(id = android.R.drawable.ic_menu_delete),
                                        contentDescription = stringResource(id = R.string.delete),
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clickable {
                                                viewModel.eliminarProducto(producto)
                                            }
                                    )
                                }

                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
    }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAppConfig(navController: NavController) {
    val viewModel:MainViewModel = viewModel()
    val context = LocalContext.current
    val ordenAlfabetico by viewModel.ordenAlfabetico.observeAsState(false)
    val mostrarPrioridadItems by viewModel.mostrarPrioridadItems.observeAsState(false)
    Column(modifier = Modifier.padding(16.dp)) {
        TopAppBar(
            title = { Text(stringResource(id = R.string.Settings))},
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = stringResource(
                        id = R.string.back
                    ))
                }
            }
        )
        ListaConfig(titulo = stringResource(id = R.string.OrderA),
                    checked = ordenAlfabetico,
                    onCheckedChange = {isChecked ->
                        viewModel.setOrdenAlfabetico(isChecked)
                        viewModel.guardarOrdenAlfabetico(context, isChecked)
                    })
        ListaConfig(titulo = stringResource(id = R.string.OrderToBuy),
                    checked = mostrarPrioridadItems,
                    onCheckedChange = {isChecked ->
                        viewModel.setOrdenPrioridad(isChecked)
                        viewModel.guardarOrdenPrioridad(context,isChecked)
                    })
    }
}
@Composable
fun ListaConfig(titulo: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = titulo, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

