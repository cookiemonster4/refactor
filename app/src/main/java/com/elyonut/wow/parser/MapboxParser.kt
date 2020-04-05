package com.elyonut.wow.parser

import com.elyonut.wow.model.FeatureModel
import com.elyonut.wow.model.PolygonModel
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon

class MapboxParser {
    companion object {
        fun parseToMapboxFeature(featureModel: FeatureModel): Feature {
            return Feature.fromGeometry(
                parseToMapboxPolygon(featureModel.geometry as PolygonModel),
                featureModel.properties,
                featureModel.id
            )
        }

        fun parseToFeatureModel(feature: Feature): FeatureModel {
            return FeatureModel(
                feature.id(),
                feature.properties(),
                parseToGeometryModel(feature.geometry() as Polygon),
                feature.type()
            )
        }

        private fun parseToMapboxPolygon(polygonModel: PolygonModel): Polygon {
            val points = ArrayList<Point>()
            polygonModel.coordinates.forEach { it ->
                it.forEach {
                    points.add(
                        Point.fromLngLat(
                            it[0],
                            it[1]
                        )
                    )
                }
            }
            val pointsList = arrayListOf(points.toList()).toList()

            return Polygon.fromLngLats(pointsList)
        }

        private fun parseToGeometryModel(polygon: Polygon): PolygonModel {
            val points = ArrayList<List<List<Double>>>()

            polygon.coordinates().forEach { it ->
                val coordinatesList = ArrayList<List<Double>>()
                it.forEach {
                    coordinatesList.add(it.coordinates())
                }

                points.add(coordinatesList)
            }

            return PolygonModel(points, polygon.type())
        }
    }
}