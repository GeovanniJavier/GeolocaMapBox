package geolocamapbox.unitec

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.mapbox.android.core.location.*
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
import java.lang.ref.WeakReference

//Para poder activar la geolocalizacion debemos implementar una nueva interface que nos permite
//manipular los sensores y servicios del dispositivo desde el codigo.
class MainActivity : AppCompatActivity(), OnMapReadyCallback, PermissionsListener {



//Primero creamos un atributo que es el tiempo en milisegundos de solicitud de locaclizacion
    private var INTERVALO_DE_DEFECTO=1000L
    private var MAXIMO_INTERVALO_ESPERA=INTERVALO_DE_DEFECTO*5;
    //Declaramos un atributo de tipo permision manager para acceder al gps si esta apagado
    private var permissionsManager:PermissionsManager= PermissionsManager(this)

    //el permiso del gps es una cosa y otra es la locaclizacion en si. para ello declaramos el atributo locationEngine
    private var locationEngine:LocationEngine?=null
    //Una vez que se autoriza el gps debemos informar al anterior objeto que se ponga a chambiar
    //El siguiente objeto le indica al LocationEngine que ya se informo del prendido o autorizacion del gps
    //A este tipo e atributos se les denomina CALLBacks
    private val callback= LocationCallbackActivity(this)

    //Creamos el atirbuto de tipo mapbox
    private lateinit var mapboxMap: MapboxMap

    //implementamos esa clasesitaaaa
     internal inner class LocationCallbackActivity(activity:MainActivity):LocationEngineCallback<LocationEngineResult>{
        //En esta parte espere el obbjeto de tipo callback para acceder a la localizacion en el rango establecido
        private var activityWeak:WeakReference<MainActivity>?=null
        init {
            activityWeak = WeakReference(activity)
        }
        //Estos metodos especialmente el onsuccess nos indica cada cambiara la locaclizacion y si fue con exito
        override fun onSuccess(result: LocationEngineResult?) {
            //Aqui ira un pocquito de codigo
            val activity: MainActivity? = activityWeak!!.get()
            if (activity != null){
                val location = result?.lastLocation ?: return

                //Cada vez que se actualice con exito una nueva locaclizacion, aparecera este Toast
                //Se puede eliminar si se desea para que no aparesca cada rato y este chingue y chingue en la pantalla
                //del usuario
                Toast.makeText(
                    activity,
                    "Lat. Actualizada javi:"+ result.lastLocation!!.latitude.toString(),
                    Toast.LENGTH_SHORT
                ).show()

                //La nueva posicion actualizada es la variable "pos"; los dobles signos de exclamacion en kotlin implican
                //que este valor NO ES NULO esta posicion es la que debe de irse agregando a un ArrayList en tu perfil.
                //ES LA QUE NOS INTERESA PARA FINE DE MONITOREO O TRACKING DEL USUARIO!, HAY QUE EMOSION!!!!!
                var pos = LatLng()
                pos.latitude=result.lastLocation!!.latitude
                pos.longitude=result.lastLocation!!.longitude
                //La varible posicion es la que va a verse reflejada y actualizada en el mapa y lo traladamos con el metodo:
                //animateCamera, puedes cambiar el zoom de la camara y el metodo "tilt", es para poner que tan inclinado
                // se muestra tu mapita para que simule que estas en 3D, es una mamada dice el profe xDD, se ve chingon e
                // impresiona a los no-ingenieros, a mi me impresiono sera que no soy ing xDD by javi
                val posicion = CameraPosition.Builder()
                    .target(pos)
                    .zoom(18.0)
                    .tilt(20.0)
                    .build()

                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(posicion), 500)

                //Pasamos la nueva localizacion actualziada al mapbox cada que se cumpla el rango de tiempo
                if (activity.mapboxMap != null && result.lastLocation != null){
                    activity.mapboxMap.getLocationComponent()
                        .forceLocationUpdate(result.lastLocation)

                }
            }
        }
        /***En caso de un fallo e igual a una excepcion dejamos al sistema de excepciones que haga su tarea, que
         * nos de la que crea conveniente
        Este informa al usuario si hubo un error por problemas de GPS***/
        override fun onFailure(exception: Exception) {
            val activity: MainActivity? = activityWeak!!.get()
            if (activity != null){
                Toast.makeText(
                    activity, exception.message,
                    Toast.LENGTH_SHORT
                ).show()

            }
        }
    }



    //Nota cambiar clave aqui
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Antes de invocar el token se debe de ahcer en esta seccion de la activityMain
        //Si no lo haces marca error al ejecutar la app
        Mapbox.getInstance(this, "pk.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")

        setContentView(R.layout.activity_main)
        //Ahora si incializamos el mapview para que contenga nuestro mapa
        mapView.onCreate(savedInstanceState)
        //Descargamos el mapa asicronicamente
        mapView.getMapAsync(this)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        //En este momento nuestro atributo mapboxMap lo asignamos al argumento mapboxmap
        this.mapboxMap=mapboxMap
        //despuess de esto deberia aparecer el mapa en las coordenadas puestas y el zoom dado
        //Pero... aqui pondremos el estilo del mapa
        mapboxMap.setStyle(Style.Builder().fromUri("mapbox://styles/javi06/ckdkw3flt1h461itop6kwdabs")){
            //Dice el profe que esto es la cereza del pastel
            habilitarLocalizacion(it)
        }
    }

    /***
     * CHECAMOS que el usuario conceda el permiso:metodo rock-start es que hace TODO !!
     * para mayor informacion de lint check verifica el siguiente link que con todo carino del profe
     * puso: https://developer.android.com/studio/write/lint
     */
    @SuppressLint("MissingPermission")
    private fun habilitarLocalizacion(loadedMapStyle: Style){
        //Checamos si los permisos han sido concedidos sino, forzara que a la de afuerza
        //los conceda, posteriormente, si es acptado y cancedido se el cuerpo del IF
        //Genermos la variable miLocalitionComponentOption que con ella agregaremos el continua
        //traiking o seguimiento asi como el color del tracking, aqui podras personalizar el color,
        //aqui le puse el color primary pero podras poner el que se te la gana.

        if(PermissionsManager.areLocationPermissionsGranted(this)){
            val miLocationComponentOptions = LocationComponentOptions.builder(this)
                .trackingGesturesManagement(true)
                .accuracyColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .build()

            val locationComponentActivationOptions = LocationComponentActivationOptions.builder(this, loadedMapStyle)
                .locationComponentOptions(miLocationComponentOptions)
                .build()

            // Aqui con la ayuda de la las bibliotecas standarizadas de kotlin  le pasamos al mapa
            //las opciones de al variable miLocationComponentOptions
            //Para saber de estas bibliotecas en koltin te paso el link oficial. Aqui usamos la funcion de
            // orden superior apply :
            //  https://kotlinlang.org/api/latest/jvm/stdlib/
            mapboxMap.locationComponent.apply {

                activateLocationComponent(locationComponentActivationOptions)

                isLocationComponentEnabled = true
                cameraMode = CameraMode.TRACKING
                //Sacamos el valor de lastKnownLocation que e este cso sria a inicial
                var loca=   lastKnownLocation
                Toast.makeText(applicationContext, "Latitud  inicial es esta javi!! ${loca?.latitude}", Toast.LENGTH_LONG).show()

                // Ajustamos el modo de Reder de la camara a Brújula e iniciamos el tracking invocanso el emtodo de
                //busqueda eta funcion esta implementada mas abajito
                renderMode = RenderMode.COMPASS
                iniciarMaquinaDeSeguimiento();
            }
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)

        }
    }

    /********************************************************************************************
    Metodo de seguimiento impornatntisimo!!
    Ya casi terminamos,  ya no te desesperes, aguanta!! querías ser ingeniero no???!!
     */
    @SuppressLint("MissingPermission")
    //En la siguiente funcion iniciamos la maquina de segumiemnto autonomo!!
    private fun iniciarMaquinaDeSeguimiento() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this)
        val request =
            LocationEngineRequest.Builder(INTERVALO_DE_DEFECTO)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(MAXIMO_INTERVALO_ESPERA).build()
        locationEngine!!.requestLocationUpdates(request, callback, mainLooper)
        locationEngine!!.getLastLocation(callback)
    }
    /***************************************************************************************************
    Termina primer bloque de código
     ******************************************************************************************************/

    // En este metodo nosotros vamos a implementar un mensaje donde el usuario verifica o se le informa
    // porqué prenderemos el GPS
    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(this, "Esta aplicacion necesita el GPS para ubicarte,", Toast.LENGTH_LONG).show()
    }

    /**************************************************************************************************
    2.- SEGUNDO BLOQUE DE CÓDIGO: cOMPLETAMOS EL onPermissionResult
     */
    override fun onPermissionResult(granted: Boolean) {
        //Se se concede el permiso entonces hanilitamos neustra localizacion con el metodo habilitarLocalizacion
        if (granted) {
            habilitarLocalizacion(mapboxMap.style!!)
        } else {
            Toast.makeText(this,"No aceptaste que te localice:CULERO!!", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    //Agregamos el metodo donde se verifica el estatus del permiso

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /************************************************************************************************************
    Termina el segundo bloque de codigo
     */

    /***************************************************************************************************
    Finalmente los bloques de manejo d memoria
     */
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

}


//Todo bien el e proyecto